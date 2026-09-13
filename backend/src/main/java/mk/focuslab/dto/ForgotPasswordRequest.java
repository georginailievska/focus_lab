package mk.focuslab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(

        @NotBlank(message = "Email-от е задолжителен")
        @Email(message = "Невалиден email")
        String email
) {
}
