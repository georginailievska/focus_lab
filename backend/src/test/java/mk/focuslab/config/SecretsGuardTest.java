package mk.focuslab.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ако овие тестови паднат, значи апликацијата пак се крева со позната тајна. */
class SecretsGuardTest {

    private static final String SHIPPED_SECRET =
            "promeni-ja-ovaa-tajna-vo-produkcija-focuslab-2026-minimum-256-bit";

    private static final String REAL_SECRET = "vistinska-tajna-dolga-najmalku-32-znaci-1234";

    private SecretsGuard guard(String jwtSecret, String adminPassword) {
        return new SecretsGuard(jwtSecret, adminPassword, "app-password");
    }

    @Test
    @DisplayName("Тајната од application.yml не поминува")
    void rejectsShippedJwtSecret() {
        assertThatThrownBy(() -> guard(SHIPPED_SECRET, "vistinska-lozinka").verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("Кратка тајна не поминува")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> guard("kratko", "vistinska-lozinka").verify())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Admin лозинката од application.yml не поминува")
    void rejectsShippedAdminPassword() {
        assertThatThrownBy(() -> guard(REAL_SECRET, "ChangeMe123!").verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("Со вистински вредности стартот поминува")
    void acceptsRealValues() {
        assertThatCode(() -> guard(REAL_SECRET, "vistinska-lozinka").verify()).doesNotThrowAnyException();
    }
}
