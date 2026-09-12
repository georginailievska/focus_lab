package mk.focuslab.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InterestsRequest(
        @NotNull(message = "Листата предмети е задолжителна")
        List<Long> subjectIds
) {
}
