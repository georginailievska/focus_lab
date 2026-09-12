package mk.focuslab.repository;

import mk.focuslab.model.PasswordResetToken;
import mk.focuslab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Ново барање ги поништува претходните — важи најмногу еден линк по корисник. */
    void deleteByUser(User user);

    /** Чистење на истечените (види {@code purgeExpiredTokens}). */
    long deleteByExpiresAtBefore(Instant moment);
}
