package mk.focuslab.dto;

import jakarta.validation.constraints.NotNull;
import mk.focuslab.model.ApplicationStatus;

public record DecisionRequest(
        @NotNull
        ApplicationStatus status
) {
}
