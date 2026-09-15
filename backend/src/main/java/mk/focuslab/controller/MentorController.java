package mk.focuslab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.ApplicationResponse;
import mk.focuslab.dto.MentorNoteResponse;
import mk.focuslab.dto.OverlapResponse;
import mk.focuslab.dto.MentorResponse;
import mk.focuslab.dto.SessionNoteRequest;
import mk.focuslab.dto.SessionNoteResponse;
import mk.focuslab.dto.SessionResponse;
import mk.focuslab.dto.StudentCommentRequest;
import mk.focuslab.dto.StudentCommentResponse;
import mk.focuslab.security.UserPrincipal;
import mk.focuslab.service.SessionNoteService;
import mk.focuslab.service.StudentCommentService;
import mk.focuslab.service.SessionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

// Целиот /api/mentor/** е веќе ограничен на hasRole("MENTOR") во SecurityConfig
@RestController
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
public class MentorController {

    // Форматот што го праќа формата; ISO со зона тука само би збркал
    private static final String LOCAL_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss";

    private final SessionService sessionService;
    private final SessionNoteService noteService;
    private final StudentCommentService commentService;

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

    // Кои сесии паѓаат во периодот што менторот го избира во формата
    @GetMapping("/overlaps")
    public ResponseEntity<List<OverlapResponse>> overlaps(
            @RequestParam @DateTimeFormat(pattern = LOCAL_DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = LOCAL_DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                sessionService.findOverlaps(startTime, endTime, sessionId, principal.getUser()));
    }

    // Сите забелешки на едно место, по избор филтрирани по предмет
    @GetMapping("/notes")
    public ResponseEntity<List<MentorNoteResponse>> allNotes(
            @RequestParam(required = false) Long subjectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(noteService.listAllNotes(subjectId, principal.getUser()));
    }

    // Пријавите на една сесија: прифатени, на чекање и одбиени
    @GetMapping("/sessions/{sessionId}/applications")
    public ResponseEntity<List<ApplicationResponse>> sessionApplications(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                sessionService.listApplicationsForSession(sessionId, principal.getUser()));
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

    // Коментари на ментори за студент; видливоста ја бира авторот
    @GetMapping("/students/{studentId}/comments")
    public ResponseEntity<List<StudentCommentResponse>> studentComments(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(commentService.listComments(studentId, principal.getUser()));
    }

    @PostMapping("/students/{studentId}/comments")
    public ResponseEntity<StudentCommentResponse> addStudentComment(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                commentService.addComment(studentId, request, principal.getUser()));
    }

    @PatchMapping("/student-comments/{commentId}")
    public ResponseEntity<StudentCommentResponse> changeCommentVisibility(
            @PathVariable Long commentId,
            @RequestParam boolean sharedWithMentors,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(commentService.changeVisibility(
                commentId, sharedWithMentors, principal.getUser()));
    }

    @DeleteMapping("/student-comments/{commentId}")
    public ResponseEntity<Void> deleteStudentComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        commentService.deleteComment(commentId, principal.getUser());
        return ResponseEntity.noContent().build();
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
