package mk.focuslab.service;

import lombok.extern.slf4j.Slf4j;
import mk.focuslab.dto.ChangePasswordRequest;
import mk.focuslab.dto.ForgotPasswordRequest;
import mk.focuslab.dto.ResetPasswordRequest;
import mk.focuslab.event.PasswordChangedEvent;
import mk.focuslab.event.PasswordResetRequestedEvent;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.model.PasswordResetToken;
import mk.focuslab.model.User;
import mk.focuslab.repository.PasswordResetTokenRepository;
import mk.focuslab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class PasswordService {
    private static final String INVALID_TOKEN =
            "Линкот за нова лозинка не е валиден или истекол. Побарај нов.";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final Duration tokenTtl;

    public PasswordService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events,
            @Value("${app.auth.password-reset-ttl-minutes:30}") long tokenTtlMinutes
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.tokenTtl = Duration.ofMinutes(tokenTtlMinutes);
    }

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> found = userRepository.findByEmailIgnoreCase(email);

        if (found.isEmpty()) {
            // Во лог, не во одговор — администраторот смее да го знае, напаѓачот не.
            log.info("Барање за нова лозинка за нерегистрирана адреса — нема што да се прати");
            return;
        }

        User user = found.get();

        tokenRepository.deleteByUser(user);

        String rawToken = newToken();
        tokenRepository.save(
                PasswordResetToken.builder()
                        .user(user)
                        .tokenHash(hash(rawToken))
                        .expiresAt(Instant.now().plus(tokenTtl))
                        .build()
        );

        events.publishEvent(new PasswordResetRequestedEvent(
                user.getEmail(), user.getFullName(), rawToken, tokenTtl.toMinutes()));

        log.info("Испратен линк за нова лозинка до корисник {}", user.getId());
    }

    /** Поставување нова лозинка преку линкот од email-от. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.token().trim()))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN));

        if (!token.isUsable(Instant.now())) {
            throw new IllegalArgumentException(INVALID_TOKEN);
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Стариот токен веднаш престанува да важи — види JwtUtil.isTokenValid
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        events.publishEvent(new PasswordChangedEvent(user.getEmail(), user.getFullName()));

        log.info("Корисник {} постави нова лозинка преку линк", user.getId());
    }

    /** Промена од профилот — тука тековната лозинка е докажување на идентитет. */
    @Transactional
    public void changePassword(User current, ChangePasswordRequest request) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Профилот не постои."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Тековната лозинка не е точна.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Новата лозинка мора да е различна од тековната.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Стариот токен веднаш престанува да важи — види JwtUtil.isTokenValid
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        // Ако некој побарал линк за оваа сметка, тој линк веќе не важи.
        tokenRepository.deleteByUser(user);

        events.publishEvent(new PasswordChangedEvent(user.getEmail(), user.getFullName()));

        log.info("Корисник {} ја смени лозинката од профилот", user.getId());
    }

    /** Секоја ноќ: истечените токени немаат зошто да стојат во базата. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        long removed = tokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) {
            log.info("Исчистени {} истечени токени за лозинка", removed);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 е задолжителен дел од Java платформата — овде не се доаѓа.
            throw new IllegalStateException("SHA-256 недостапен", e);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
