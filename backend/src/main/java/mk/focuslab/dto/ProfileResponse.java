package mk.focuslab.dto;

import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;

import java.time.Instant;
import java.util.List;

public record ProfileResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        MentorStatus mentorStatus,
        Instant createdAt,
        String avatarUrl,
        List<SubjectResponse> interests,
        ProfileStats stats
) {
}
