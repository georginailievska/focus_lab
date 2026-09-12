package mk.focuslab.controller;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.AvatarData;
import mk.focuslab.service.ProfileService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/avatars")
@RequiredArgsConstructor
public class AvatarController {
    private final ProfileService profileService;

    @GetMapping("/{key}")
    public ResponseEntity<byte[]> avatar(@PathVariable String key) {
        AvatarData avatar = profileService.avatarByKey(key);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(avatar.data());
    }
}
