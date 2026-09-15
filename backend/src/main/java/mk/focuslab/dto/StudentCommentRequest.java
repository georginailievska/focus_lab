package mk.focuslab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mk.focuslab.model.StudentComment;

public record StudentCommentRequest(

        @NotBlank(message = "Коментарот не може да биде празен")
        @Size(max = StudentComment.MAX_TEXT_LENGTH, message = "Најмногу 1000 знаци")
        String text,

        // Без default: видливоста е одлука на авторот и мора да дојде во барањето
        @NotNull(message = "Избери кој смее да го гледа коментарот")
        Boolean sharedWithMentors
) {
}
