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

    /** Патека → (колку барања, во колкав прозорец). */
    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/auth/login", new Limit(10, Duration.ofMinutes(5)),
            "/api/auth/register", new Limit(5, Duration.ofHours(1)),
            "/api/auth/forgot-password", new Limit(5, Duration.ofHours(1)),
            "/api/auth/reset-password", new Limit(10, Duration.ofHours(1))
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

        if (!allow(key, limit)) {
            log.warn("Премногу барања кон {} од еден клиент — одбиено", path);
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean allow(String key, Limit limit) {
        if (hits.size() > MAX_TRACKED) {
            hits.clear();
        }

        Deque<Instant> window = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Instant cutoff = Instant.now().minus(limit.window());

        // Синхронизирано по клучот: два истовремени обиди не смеат да го пропуштат бројот
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.pollFirst();
            }

            if (window.size() >= limit.maxRequests()) {
                return false;
            }

            window.addLast(Instant.now());
            return true;
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Премногу обиди. Пробај повторно подоцна.\"}");
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
