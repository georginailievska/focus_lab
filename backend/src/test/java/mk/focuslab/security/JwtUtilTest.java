package mk.focuslab.security;

import mk.focuslab.model.Role;
import mk.focuslab.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Токенот е единственото нешто што стои меѓу барањето и податоците. */
class JwtUtilTest {

    private static final String SECRET = "test-tajna-dolga-najmalku-32-znaci-za-hs256";
    private static final long ONE_HOUR = 3_600_000L;

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);

    private User user(int tokenVersion) {
        return User.builder()
                .id(5L)
                .fullName("Ана Николоска")
                .email("ana@students.finki.ukim.mk")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .tokenVersion(tokenVersion)
                .build();
    }

    @Test
    @DisplayName("Свеж токен важи за својот корисник")
    void freshTokenIsValid() {
        User user = user(0);

        assertThat(jwtUtil.isTokenValid(jwtUtil.generateToken(user), new UserPrincipal(user))).isTrue();
    }

    @Test
    @DisplayName("Промената на лозинка го поништува стариот токен")
    void tokenFromBeforePasswordChangeIsRejected() {
        User before = user(0);
        String oldToken = jwtUtil.generateToken(before);

        // истиот корисник по промена на лозинка — верзијата е зголемена
        User after = user(1);

        assertThat(jwtUtil.isTokenValid(oldToken, new UserPrincipal(after))).isFalse();
    }

    @Test
    @DisplayName("Токен потпишан со друга тајна не важи")
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtUtil attacker = new JwtUtil("napadacka-tajna-isto-dolga-najmalku-32-znaci", ONE_HOUR);
        User user = user(0);

        assertThat(jwtUtil.isTokenValid(attacker.generateToken(user), new UserPrincipal(user))).isFalse();
    }

    @Test
    @DisplayName("Истечен токен не важи")
    void expiredTokenIsRejected() {
        JwtUtil expiring = new JwtUtil(SECRET, -1_000L);
        User user = user(0);

        assertThat(jwtUtil.isTokenValid(expiring.generateToken(user), new UserPrincipal(user))).isFalse();
    }

    @Test
    @DisplayName("Измислен текст наместо токен не крши ништо")
    void garbageIsRejected() {
        User user = user(0);

        assertThat(jwtUtil.isTokenValid("ова.не.е.токен", new UserPrincipal(user))).isFalse();
        assertThat(jwtUtil.isTokenValid("", new UserPrincipal(user))).isFalse();
    }
}
