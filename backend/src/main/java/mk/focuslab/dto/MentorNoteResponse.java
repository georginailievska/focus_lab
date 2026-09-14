package mk.focuslab.dto;

import java.time.Instant;
import java.time.LocalDateTime;

/** Забелешка во прегледот на сите забелешки — носи и од која сесија е. */
public record MentorNoteResponse(
        Long id,
        AuthorResponse author,
        String text,
        Instant createdAt,
        Long sessionId,
        String sessionTitle,
        LocalDateTime sessionStartTime,
        SubjectResponse subject
) {
}
