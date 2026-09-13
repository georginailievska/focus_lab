package mk.focuslab.event;

import java.util.List;

public record SessionUpdatedEvent(
        Long sessionId,
        String sessionTitle,
        List<String> changes,
        List<Recipient> recipients
) {
}
