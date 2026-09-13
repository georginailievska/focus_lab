package mk.focuslab.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mk.focuslab.model.SessionMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record SessionRequest(

        @NotBlank(message = "Насловот е задолжителен")
        String title,

        String description,

        @NotNull(message = "Предметот е задолжителен")
        Long subjectId,

        // Деловно правило: 1 или 2 ментори по сесија (се проверува во SessionService)
        @NotEmpty(message = "Мора да има барем еден ментор")
        @Size(max = 2, message = "Најмногу 2 ментори по сесија")
        List<Long> mentorIds,

        @NotNull
        SessionMode mode,

        String location,

        @NotNull(message = "Времето на почеток е задолжително")
        @Future(message = "Сесијата мора да биде закажана во иднина")
        LocalDateTime startTime,

        @NotNull(message = "Времето на крај е задолжително")
        LocalDateTime endTime,

        Set<String> tags
) {
}
