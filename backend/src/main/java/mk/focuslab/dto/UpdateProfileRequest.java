package mk.focuslab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Името е задолжително")
        @Size(min = 3, max = 100, message = "Името мора да има од 3 до 100 карактери")
        String fullName
) {
}
