package mk.focuslab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mk.focuslab.model.Post;

public record PostRequest(

        @NotBlank(message = "Објавата не може да биде празна")
        @Size(max = Post.MAX_TEXT_LENGTH, message = "Најмногу 2000 знаци")
        String text,

        /** Опционален предмет — не секоја објава е за конкретен предмет. */
        Long subjectId
) {
}
