package mk.focuslab.dto;

import mk.focuslab.model.ApplicationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        UserResponse student,
        Long sessionId,
        String sessionTitle,
        LocalDateTime sessionStartTime,
        ApplicationStatus status,
        Instant appliedAt,
        Instant decidedAt
) {
}
