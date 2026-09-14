package mk.focuslab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.ApplicationResponse;
import mk.focuslab.dto.MentorNoteResponse;
import mk.focuslab.dto.MentorResponse;
import mk.focuslab.dto.SessionNoteRequest;
import mk.focuslab.dto.SessionNoteResponse;
import mk.focuslab.dto.SessionResponse;
import mk.focuslab.security.UserPrincipal;
import mk.focuslab.service.SessionNoteService;
import mk.focuslab.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Целиот /api/mentor/** е веќе ограничен на hasRole("MENTOR") во SecurityConfig
@RestController
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
public class MentorController {
    private final SessionService sessionService;
    private final SessionNoteService noteService;

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

    // Сите забелешки на едно место, по избор филтрирани по предмет
    @GetMapping("/notes")
    public ResponseEntity<List<MentorNoteResponse>> allNotes(
            @RequestParam(required = false) Long subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(noteService.listAllNotes(subjectId, principal.getUser()));
    }

    @GetMapping("/sessions/{sessionId}/notes")
    public ResponseEntity<List<SessionNoteResponse>> notes(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(noteService.listNotes(sessionId, principal.getUser()));
    }

    @PostMapping("/sessions/{sessionId}/notes")
    public ResponseEntity<SessionNoteResponse> addNote(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(noteService.addNote(sessionId, request, principal.getUser()));
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        noteService.deleteNote(noteId, principal.getUser());
        return ResponseEntity.noContent().build();
    }
}
