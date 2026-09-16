package mk.focuslab.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Известување во ѕвончето. Секој ред е за еден корисник.
 *
 * <p>Сесијата е запишана само како број, без врска кон таблицата: откажаната
 * сесија се брише, а известувањето „сесијата е откажана" треба да остане.
 */
@Entity
@Table(
        name = "notifications",
        indexes = @Index(name = "idx_notifications_recipient", columnList = "recipient_id, created_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    public static final int MAX_TITLE_LENGTH = 160;
    public static final int MAX_BODY_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(length = MAX_BODY_LENGTH)
    private String body;

    /** Сесијата за која е известувањето, ако сè уште може да се отвори. */
    @Column(name = "session_id")
    private Long sessionId;

    /** null значи непрочитано. */
    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
