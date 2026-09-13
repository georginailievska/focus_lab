package mk.focuslab.dto;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank(message = "Името на предметот е задолжително")
        String name
) {
}
