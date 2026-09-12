package mk.focuslab.event;

public record ApplicationReceivedEvent(
        String studentEmail,
        String studentName,
        String sessionTitle
) {
}
