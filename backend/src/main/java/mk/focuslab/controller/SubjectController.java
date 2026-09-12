package mk.focuslab.controller;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.SubjectResponse;
import mk.focuslab.service.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> listSubjects() {
        return ResponseEntity.ok(subjectService.listSubjects());
    }
}
