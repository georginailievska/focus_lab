package mk.focuslab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.ChangePasswordRequest;
import mk.focuslab.dto.ProfileResponse;
import mk.focuslab.dto.UpdateProfileRequest;
import mk.focuslab.security.UserPrincipal;
import mk.focuslab.service.PasswordService;
import mk.focuslab.service.ProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    private final PasswordService passwordService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> myProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(profileService.myProfile(principal.getUser()));
    }

    @PatchMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(profileService.updateName(principal.getUser(), request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> uploadAvatar(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(profileService.uploadAvatar(principal.getUser(), file));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<ProfileResponse> removeAvatar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(profileService.removeAvatar(principal.getUser()));
    }

    /** Промена на лозинка од профилот (со потврда на тековната). */
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        passwordService.changePassword(principal.getUser(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ProfileResponse> userProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(profileService.profileOf(id, principal.getUser()));
    }
}
