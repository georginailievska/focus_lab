package mk.focuslab.repository;

import mk.focuslab.model.Notification;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Limit limit);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    /**
     * Означува прочитано само ако редот е на тој корисник.
     *
     * <p>Условот за примачот е тука, а не во сервисот: така туѓо id не менува
     * ништо, а одговорот е ист — не се дознава дали таквото известување постои.
     */
    @Modifying
    @Query("""
            update Notification n set n.readAt = :now
            where n.id = :id and n.recipient.id = :recipientId and n.readAt is null
            """)
    int markRead(@Param("id") Long id, @Param("recipientId") Long recipientId, @Param("now") Instant now);

    @Modifying
    @Query("""
            update Notification n set n.readAt = :now
            where n.recipient.id = :recipientId and n.readAt is null
            """)
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);

    /** Прочитаните стари известувања не се потребни никому. */
    @Modifying
    @Query("delete from Notification n where n.readAt is not null and n.createdAt < :cutoff")
    int deleteReadOlderThan(@Param("cutoff") Instant cutoff);
}
