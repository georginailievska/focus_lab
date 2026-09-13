package mk.focuslab.dto;

import mk.focuslab.model.Role;

public record AuthorResponse(
        Long id,
        String fullName,
        Role role,
        String avatarUrl
) {
}
