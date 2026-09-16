package mk.focuslab.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import mk.focuslab.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("tokenVersion", user.getTokenVersion())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Токенот важи ако потписот е наш, адресата се совпаѓа, не е истечен и
     * носи тековната верзија на корисникот. Последното е она што прави
     * промената на лозинка веднаш да го поништи стариот токен.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            if (!(userDetails instanceof UserPrincipal principal)) {
                return false;
            }

            String email = extractEmail(token);

            return email.equals(principal.getUsername())
                    && !isExpired(token)
                    && extractTokenVersion(token) == principal.getUser().getTokenVersion();
        } catch (Exception e) {
            return false;
        }
    }

    /** Токен издаден пред полето да постоеше нема claim — тој се смета за стар. */
    private int extractTokenVersion(String token) {
        Integer version = extractClaim(token, claims -> claims.get("tokenVersion", Integer.class));
        return version == null ? -1 : version;
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
