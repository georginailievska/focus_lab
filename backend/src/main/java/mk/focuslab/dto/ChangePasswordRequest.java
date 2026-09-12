package mk.focuslab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Тековната лозинка е задолжителна")
        String currentPassword,

        @NotBlank(message = "Новата лозинка е задолжителна")
        @Size(min = 8, message = "Лозинката мора да има најмалку 8 карактери")
        String newPassword
) {
}
