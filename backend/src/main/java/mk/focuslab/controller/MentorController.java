package mk.focuslab.controller;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.ApplicationResponse;
import mk.focuslab.dto.MentorResponse;
import mk.focuslab.dto.SessionResponse;
import mk.focuslab.security.UserPrincipal;
import mk.focuslab.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Целиот /api/mentor/** е веќе ограничен на hasRole("MENTOR") во SecurityConfig
@RestController
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
public class MentorController {
    private final SessionService sessionService;

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> mySessions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(sessionService.listSessionsForMentor(principal.getUser()));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<ApplicationResponse>> pendingRequests(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(sessionService.listPendingRequestsForMentor(principal.getUser()));
    }

    // Останати одобрени ментори — за избор на ко-ментор при креирање сесија
    @GetMapping("/colleagues")
    public ResponseEntity<List<MentorResponse>> colleagues(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(sessionService.listApprovedMentorsExcept(principal.getUser()));
    }
}
