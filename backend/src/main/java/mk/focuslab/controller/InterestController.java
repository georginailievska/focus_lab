package mk.focuslab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.InterestsRequest;
import mk.focuslab.dto.SubjectResponse;
import mk.focuslab.security.UserPrincipal;
import mk.focuslab.service.InterestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {
    private final InterestService interestService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<SubjectResponse>> myInterests(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interestService.listInterests(principal.getUser()));
    }

    /** Замена на целата листа — види InterestsRequest зошто PUT, а не POST/DELETE по предмет. */
    @PutMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<SubjectResponse>> replaceInterests(
            @Valid @RequestBody InterestsRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(interestService.replaceInterests(principal.getUser(), request));
    }
}
