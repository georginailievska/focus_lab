package mk.focuslab.service;

import mk.focuslab.dto.ChangePasswordRequest;
import mk.focuslab.dto.ForgotPasswordRequest;
import mk.focuslab.dto.ResetPasswordRequest;
import mk.focuslab.event.PasswordChangedEvent;
import mk.focuslab.event.PasswordResetRequestedEvent;
import mk.focuslab.model.PasswordResetToken;
import mk.focuslab.model.Role;
import mk.focuslab.model.User;
import mk.focuslab.repository.PasswordResetTokenRepository;
import mk.focuslab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {
    private static final long TTL_MINUTES = 30;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher events;

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(
                userRepository, tokenRepository, passwordEncoder, events, TTL_MINUTES);
    }

    private User user() {
        return User.builder()
                .id(7L)
                .fullName("Ана Николоска")
                .email("ana@students.finki.ukim.mk")
                .passwordHash("$2a$stara")
                .role(Role.STUDENT)
                .build();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Барањето чува само отпечаток, а токенот го праќа само во настанот")
    void requestResetStoresOnlyHash() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase("ana@students.finki.ukim.mk"))
                .thenReturn(Optional.of(user));

        passwordService.requestReset(new ForgotPasswordRequest("  ANA@students.finki.ukim.mk "));

        ArgumentCaptor<PasswordResetToken> savedToken = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(savedToken.capture());

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());

        PasswordResetRequestedEvent event = (PasswordResetRequestedEvent) published.getValue();

        assertThat(event.token()).isNotBlank();
        assertThat(savedToken.getValue().getTokenHash())
                .isEqualTo(sha256(event.token()))
                .isNotEqualTo(event.token());
        assertThat(savedToken.getValue().getExpiresAt())
                .isAfter(Instant.now().plus(TTL_MINUTES - 1, ChronoUnit.MINUTES));
        assertThat(event.validMinutes()).isEqualTo(TTL_MINUTES);

        // Ново барање ги поништува претходните линкови
        verify(tokenRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("Непозната адреса не прави ништо — и не се разликува од позната")
    void requestResetStaysSilentForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("nepoznat@students.finki.ukim.mk"))
                .thenReturn(Optional.empty());

        // Без исклучок: одговорот кон корисникот е ист како за регистрирана адреса
        passwordService.requestReset(new ForgotPasswordRequest("nepoznat@students.finki.ukim.mk"));

        verify(tokenRepository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Непознат токен не поставува лозинка")
    void resetRejectsUnknownToken() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordService.resetPassword(new ResetPasswordRequest("izmislen", "novaLozinka1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не е валиден или истекол");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Истечен токен не поставува лозинка")
    void resetRejectsExpiredToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user())
                .tokenHash(sha256("tokenot"))
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(tokenRepository.findByTokenHash(sha256("tokenot"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordService.resetPassword(new ResetPasswordRequest("tokenot", "novaLozinka1")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Веќе искористен токен не работи втор пат")
    void resetRejectsUsedToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user())
                .tokenHash(sha256("tokenot"))
                .expiresAt(Instant.now().plusSeconds(600))
                .usedAt(Instant.now().minusSeconds(5))
                .build();
        when(tokenRepository.findByTokenHash(sha256("tokenot"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordService.resetPassword(new ResetPasswordRequest("tokenot", "novaLozinka1")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Важечки токен поставува нова лозинка и се затвора")
    void resetSetsNewPasswordAndBurnsToken() {
        User user = user();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(sha256("tokenot"))
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(tokenRepository.findByTokenHash(sha256("tokenot"))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("novaLozinka1")).thenReturn("$2a$nova");

        passwordService.resetPassword(new ResetPasswordRequest("  tokenot  ", "novaLozinka1"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$nova");
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.isUsable(Instant.now())).isFalse();
        verify(events).publishEvent(any(PasswordChangedEvent.class));
    }

    @Test
    @DisplayName("Промена од профилот бара точна тековна лозинка")
    void changeRequiresCorrectCurrentPassword() {
        User user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pogresna", "$2a$stara")).thenReturn(false);

        assertThatThrownBy(() -> passwordService.changePassword(
                user, new ChangePasswordRequest("pogresna", "novaLozinka1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Тековната лозинка не е точна");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Новата лозинка не смее да е истата")
    void changeRejectsSamePassword() {
        User user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("staraLozinka1", "$2a$stara")).thenReturn(true);

        assertThatThrownBy(() -> passwordService.changePassword(
                user, new ChangePasswordRequest("staraLozinka1", "staraLozinka1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("различна");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успешна промена ги поништува и линковите за нова лозинка")
    void changeInvalidatesOutstandingResetLinks() {
        User user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("staraLozinka1", "$2a$stara")).thenReturn(true);
        when(passwordEncoder.matches("novaLozinka1", "$2a$stara")).thenReturn(false);
        when(passwordEncoder.encode("novaLozinka1")).thenReturn("$2a$nova");

        passwordService.changePassword(user, new ChangePasswordRequest("staraLozinka1", "novaLozinka1"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$nova");
        verify(tokenRepository).deleteByUser(user);
        verify(events).publishEvent(any(PasswordChangedEvent.class));
    }
}
