package mk.focuslab.dto;

import java.time.Instant;

public record StudentCommentResponse(
        Long id,
        AuthorResponse author,
        String text,
        boolean sharedWithMentors,
        Instant createdAt
) {
}
