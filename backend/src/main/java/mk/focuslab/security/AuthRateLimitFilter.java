package mk.focuslab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ограничување на барањата кон `/api/auth/**`.
 *
 * <p>Без ова, најавата е отворена за пробување лозинки во недоглед, а
 * регистрацијата е бесплатен начин да се провери која email адреса има профил.
 * Броењето е во меморија — доволно за еден сервер; со повеќе инстанци ова оди
 * во Redis, инаку секоја инстанца брои свое.
 */
@Component
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    /**
     * Патека → (колку барања, во колкав прозорец).
     *
     * <p>Бројките се намерно широки. Броењето е по IP адреса, а цел факултет
     * излегува на интернет преку неколку адреси — со тесен лимит, една вежба
     * во која дваесет студенти се најавуваат истовремено би се блокирала сама.
     * Лимитот тука сопира машинско пробување, не нормална употреба.
     */
    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/auth/login", new Limit(20, Duration.ofMinutes(5)),
            "/api/auth/register", new Limit(20, Duration.ofHours(1)),
            "/api/auth/forgot-password", new Limit(10, Duration.ofHours(1)),
            "/api/auth/reset-password", new Limit(20, Duration.ofHours(1))
    );

    /**
     * Патеки каде се брои само НЕуспешниот обид.
     *
     * <p>Кај најавата се брои само погрешна лозинка. Инаку успешните најави го
     * трошат истиот буџет, па цела просторија на иста адреса се блокира сама
     * среде час — а тоа не сопира никого што пробива лозинки.
     */
    private static final Map<String, Integer> COUNT_ONLY_WHEN_STATUS = Map.of(
            "/api/auth/login", HttpStatus.UNAUTHORIZED.value()
    );

    /** Колку различни клиенти се паметат пред да се исчисти мапата. */
    private static final int MAX_TRACKED = 10_000;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final boolean trustForwardedFor;

    public AuthRateLimitFilter(
            @Value("${app.security.trust-forwarded-for:false}") boolean trustForwardedFor
    ) {
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LIMITS.containsKey(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getServletPath();
        Limit limit = LIMITS.get(path);
        String key = path + "|" + clientIp(request);

        if (isBlocked(key, limit)) {
            log.warn("Премногу барања кон {} од еден клиент — одбиено", path);
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);

        Integer countedStatus = COUNT_ONLY_WHEN_STATUS.get(path);

        if (countedStatus == null) {
            record(key);
        } else if (response.getStatus() == countedStatus) {
            record(key);
        } else if (response.getStatus() < 400) {
            // Успешна најава: буџетот се враќа на нула
            hits.remove(key);
        }
    }

    private boolean isBlocked(String key, Limit limit) {
        Deque<Instant> window = hits.get(key);

        if (window == null) {
            return false;
        }

        Instant cutoff = Instant.now().minus(limit.window());

        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.pollFirst();
            }

            return window.size() >= limit.maxRequests();
        }
    }

    private void record(String key) {
        if (hits.size() > MAX_TRACKED) {
            hits.clear();
        }

        Deque<Instant> window = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        // Синхронизирано по клучот: два истовремени обиди не смеат да го изгубат бројот
        synchronized (window) {
            window.addLast(Instant.now());
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Премногу неуспешни обиди. Пробај повторно за неколку минути.\"}");
    }

    /**
     * Адресата на клиентот. `X-Forwarded-For` се верува само кога апликацијата
     * навистина стои зад proxy — инаку секој би можел да го прати сам и да го
     * заобиколи ограничувањето со измислена адреса.
     */
    private String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");

            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private record Limit(int maxRequests, Duration window) {
    }
}
