package mk.focuslab.event;

import java.time.LocalDateTime;
import java.util.List;

public record SessionCancelledEvent(
        String sessionTitle,
        LocalDateTime startTime,
        List<Recipient> recipients
) {
}
