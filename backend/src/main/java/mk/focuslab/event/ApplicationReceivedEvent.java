package mk.focuslab.event;

public record ApplicationReceivedEvent(
        Long studentId,
        String studentEmail,
        String studentName,
        Long sessionId,
        String sessionTitle
) {
}
