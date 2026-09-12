package mk.focuslab.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        AuthorResponse author,
        String text,
        Instant createdAt
) {
}
