package mk.focuslab.dto;

import java.time.Instant;
import java.util.List;

public record PostResponse(
        Long id,
        AuthorResponse author,
        SubjectResponse subject,
        String text,
        List<AttachmentResponse> attachments,
        List<CommentResponse> comments,
        Instant createdAt
) {
}
