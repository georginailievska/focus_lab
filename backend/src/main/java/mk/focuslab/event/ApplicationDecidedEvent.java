package mk.focuslab.event;

import java.time.LocalDateTime;

public record ApplicationDecidedEvent(
        Long studentId,
        String studentEmail,
        String studentName,
        Long sessionId,
        String sessionTitle,
        LocalDateTime sessionStartTime,
        boolean accepted
) {
}
