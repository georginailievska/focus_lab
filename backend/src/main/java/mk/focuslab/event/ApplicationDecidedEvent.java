package mk.focuslab.event;

import java.time.LocalDateTime;

public record ApplicationDecidedEvent(
        String studentEmail,
        String studentName,
        Long sessionId,
        String sessionTitle,
        LocalDateTime sessionStartTime,
        boolean accepted
) {
}
