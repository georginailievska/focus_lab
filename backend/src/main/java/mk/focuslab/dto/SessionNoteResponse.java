package mk.focuslab.dto;

import java.time.Instant;

public record SessionNoteResponse(
        Long id,
        AuthorResponse author,
        String text,
        Instant createdAt
) {
}
