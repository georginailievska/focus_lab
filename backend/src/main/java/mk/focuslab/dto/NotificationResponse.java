package mk.focuslab.dto;

import mk.focuslab.model.NotificationType;

import java.time.Instant;

/** {@code sessionId} е null кога сесијата повеќе не постои (откажана). */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        Long sessionId,
        boolean read,
        Instant createdAt
) {
}
