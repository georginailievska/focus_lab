package mk.focuslab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.ApplicationResponse;
import mk.focuslab.dto.DecisionRequest;
import mk.focuslab.dto.SessionRequest;
import mk.focuslab.dto.SessionResponse;
import mk.focuslab.security.UserPrincipal;
import mk.focuslab.service.SessionApplicationService;
import mk.focuslab.service.SessionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;
    private final SessionApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> listSessions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                sessionService.listSessions(subjectId, from, to, principal.getUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(sessionService.getSession(id, principal.getUser()));
    }

    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ApplicationResponse>> myApplications(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(sessionService.listApplicationsForStudent(principal.getUser()));
    }

    @PostMapping
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody SessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(sessionService.createSession(request, principal.getUser()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<SessionResponse> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(sessionService.updateSession(id, request, principal.getUser()));
    }

    /** Откажување на сесија — пријавените студенти добиваат известување. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<Void> cancelSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        sessionService.cancelSession(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(sessionService.applyToSession(id, principal.getUser()));
    }

    @DeleteMapping("/{id}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        sessionService.cancelApplication(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> decide(
            @PathVariable Long applicationId,
            @Valid @RequestBody DecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(applicationService.decide(applicationId, request, principal.getUser()));
    }
}
