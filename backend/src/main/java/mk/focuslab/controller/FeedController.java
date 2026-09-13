package mk.focuslab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.CommentRequest;
import mk.focuslab.dto.FileDownload;
import mk.focuslab.dto.PostRequest;
import mk.focuslab.dto.PostResponse;
import mk.focuslab.security.UserPrincipal;
import mk.focuslab.service.FeedService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    @GetMapping("/api/posts")
    public ResponseEntity<List<PostResponse>> feed(
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long subjectId
    ) {
        return ResponseEntity.ok(feedService.listFeed(before, subjectId));
    }

    @PostMapping(value = "/api/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> create(
            @Valid @RequestPart("post") PostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(feedService.createPost(principal.getUser(), request, files));
    }

    @PostMapping("/api/posts/{id}/comments")
    public ResponseEntity<PostResponse> comment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(feedService.addComment(id, principal.getUser(), request));
    }

    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        feedService.deletePost(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/posts/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        feedService.deleteComment(commentId, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/attachments/{key}")
    public ResponseEntity<byte[]> attachment(@PathVariable String key) {
        FileDownload file = feedService.attachment(key);

        ContentDisposition disposition = (file.inline()
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", disposition.toString())
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(file.data());
    }
}
