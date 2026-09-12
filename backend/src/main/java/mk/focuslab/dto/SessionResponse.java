package mk.focuslab.dto;

import mk.focuslab.model.SessionMode;

import java.time.LocalDateTime;
import java.util.List;

public record SessionResponse(
        Long id,
        String title,
        String description,
        SubjectResponse subject,

        // Само id + име (види MentorResponse) — email-ите на менторите не излегуваат од API-то
        List<MentorResponse> mentors,

        SessionMode mode,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,

        long applicantsCount,
        int maxApplicants,
        long approvedCount,
        int maxApproved,

        // Сортирана листа (не Set) за да е редоследот стабилен меѓу повици
        List<String> tags
) {
}
