package mk.focuslab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.AdminStatsResponse;
import mk.focuslab.dto.SubjectRequest;
import mk.focuslab.dto.SubjectResponse;
import mk.focuslab.dto.UserResponse;
import mk.focuslab.model.MentorStatus;
import mk.focuslab.service.AdminService;
import mk.focuslab.service.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Целиот /api/admin/** е веќе ограничен на hasRole("ADMIN") во SecurityConfig
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final SubjectService subjectService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/mentors/pending")
    public ResponseEntity<List<UserResponse>> pendingMentors() {
        return ResponseEntity.ok(adminService.listPendingMentors());
    }

    @PatchMapping("/mentors/{id}/approve")
    public ResponseEntity<UserResponse> approveMentor(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.decideMentor(id, MentorStatus.APPROVED));
    }

    @PatchMapping("/mentors/{id}/reject")
    public ResponseEntity<UserResponse> rejectMentor(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.decideMentor(id, MentorStatus.REJECTED));
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectResponse>> subjects() {
        return ResponseEntity.ok(subjectService.listSubjects());
    }

    @PostMapping("/subjects")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.createSubject(request));
    }
}
