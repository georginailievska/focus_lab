package mk.focuslab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mk.focuslab.model.PostComment;

public record CommentRequest(

        @NotBlank(message = "Коментарот не може да биде празен")
        @Size(max = PostComment.MAX_TEXT_LENGTH, message = "Најмногу 1000 знаци")
        String text
) {
}
