package mk.focuslab.dto;

import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;

public record AuthResponse(
        String token,
        Long id,
        String fullName,
        String email,
        Role role,
        MentorStatus mentorStatus,
        String avatarUrl
) {
}
