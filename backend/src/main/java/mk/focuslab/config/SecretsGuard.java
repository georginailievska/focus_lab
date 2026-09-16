package mk.focuslab.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Не дозволува апликацијата да се стартува со тајните од `application.yml`.
 *
 * <p>Тие вредности стојат во репото за да може проектот да се клонира и пушти,
 * но тоа значи што секој што го прочитал репото ги знае. Со позната тајна за
 * JWT секој може да си потпише токен со улога ADMIN, а со позната admin
 * лозинка да се најави. Затоа стартот паѓа ако не се сменети.
 */
@Component
@Slf4j
public class SecretsGuard {

    /** Вредностите што стојат како placeholder во application.yml. */
    private static final List<String> SHIPPED_JWT_SECRETS = List.of(
            "promeni-ja-ovaa-tajna-vo-produkcija-focuslab-2026-minimum-256-bit"
    );

    private static final List<String> SHIPPED_ADMIN_PASSWORDS = List.of(
            "ChangeMe123!",
            "promeni-me-silna-lozinka"
    );

    /** HS256 бара клуч од 256 бита; пократок клуч е послаб од самиот алгоритам. */
    private static final int MIN_SECRET_LENGTH = 32;

    private final String jwtSecret;
    private final String adminPassword;
    private final String mailPassword;

    public SecretsGuard(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.admin.password}") String adminPassword,
            @Value("${spring.mail.password:}") String mailPassword
    ) {
        this.jwtSecret = jwtSecret;
        this.adminPassword = adminPassword;
        this.mailPassword = mailPassword;
    }

    @PostConstruct
    void verify() {
        if (SHIPPED_JWT_SECRETS.contains(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET е сè уште вредноста од application.yml. Таа стои во репото, "
                            + "значи секој може да потпише токен со улога ADMIN. "
                            + "Генерирај нова со `openssl rand -base64 48` и стави ја во .env.");
        }

        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET е покус од " + MIN_SECRET_LENGTH + " знаци и не е доволен за HS256.");
        }

        if (SHIPPED_ADMIN_PASSWORDS.contains(adminPassword)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD е сè уште вредноста од application.yml. "
                            + "Стави вистинска лозинка во .env пред стартување.");
        }

        // Пораката е само предупредување: локално се работи и без вистински SMTP
        if (mailPassword.isBlank() || "your-app-password".equals(mailPassword)) {
            log.warn("MAIL_PASSWORD не е поставен — известувањата по email нема да се праќаат.");
        }
    }
}
