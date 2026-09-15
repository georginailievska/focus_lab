package mk.focuslab.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Сесија што се преклопува со периодот што менторот го избира. */
public record OverlapResponse(
        Long sessionId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SubjectResponse subject,
        List<MentorResponse> mentors,
        boolean mine
) {
}
