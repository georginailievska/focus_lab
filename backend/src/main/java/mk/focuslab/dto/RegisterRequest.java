package mk.focuslab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mk.focuslab.model.Role;

public record RegisterRequest(

        @NotBlank(message = "Името е задолжително")
        String fullName,

        @NotBlank(message = "Email-от е задолжителен")
        @Email(message = "Невалиден email")
        String email,

        @NotBlank(message = "Лозинката е задолжителна")
        @Size(min = 8, message = "Лозинката мора да има најмалку 8 карактери")
        String password,

        // STUDENT или MENTOR — ADMIN не се регистрира преку оваа рута
        @NotNull(message = "Улогата е задолжителна")
        Role role
) {
}
