package mk.focuslab.dto;

import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        MentorStatus mentorStatus,
        Instant createdAt,
        /** Патека до профилната слика, или {@code null} ако корисникот нема качена. */
        String avatarUrl
) {
}
