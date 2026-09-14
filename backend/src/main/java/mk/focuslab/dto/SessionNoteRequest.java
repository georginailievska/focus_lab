package mk.focuslab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mk.focuslab.model.SessionNote;

public record SessionNoteRequest(

        @NotBlank(message = "Забелешката не може да биде празна")
        @Size(max = SessionNote.MAX_TEXT_LENGTH, message = "Најмногу 1000 знаци")
        String text
) {
}
