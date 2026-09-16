package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.focuslab.dto.AvatarData;
import mk.focuslab.dto.ProfileResponse;
import mk.focuslab.dto.ProfileStats;
import mk.focuslab.dto.SubjectResponse;
import mk.focuslab.dto.UpdateProfileRequest;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.ApplicationStatus;
import mk.focuslab.model.ProfileImage;
import mk.focuslab.model.Role;
import mk.focuslab.model.User;
import mk.focuslab.repository.ProfileImageRepository;
import mk.focuslab.repository.SessionApplicationRepository;
import mk.focuslab.repository.SessionRepository;
import mk.focuslab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProfileService {
    /** Страна на зачуваната слика во пиксели — доволно за приказ, малку на диск. */
    private static final int MAX_SIDE = 512;

    /** Горна граница на качената датотека (пред обработка). */
    private static final long MAX_UPLOAD_BYTES = 2L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif");

    private static final String STORED_CONTENT_TYPE = "image/jpeg";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ProfileImageRepository profileImageRepository;
    private final SessionRepository sessionRepository;
    private final SessionApplicationRepository applicationRepository;
    private final DtoMapper mapper;

    // ---- Читање -----------------------------------------------------------

    /** Мојот профил — со email и интереси. */
    public ProfileResponse myProfile(User current) {
        User user = loadWithInterests(current.getId());
        return mapper.toProfileResponse(user, user.getEmail(), interestsOf(user), statsOf(user));
    }

    public ProfileResponse profileOf(Long userId, User viewer) {
        boolean own = userId.equals(viewer.getId());

        if (own) {
            return myProfile(viewer);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Не постои корисник со ID " + userId));

        requireCanView(user, viewer);

        String visibleEmail = viewer.getRole() == Role.ADMIN ? user.getEmail() : null;

        return mapper.toProfileResponse(user, visibleEmail, null, statsOf(user));
    }

    /**
     * Студентот отвора само профил на ментор. Без ова, кој и да е најавен
     * можеше да ги излиста сите профили редејќи ID-а во адресата.
     */
    private void requireCanView(User user, User viewer) {
        if (viewer.getRole() != Role.STUDENT || user.getRole() == Role.MENTOR) {
            return;
        }

        throw new ForbiddenActionException("Профилот не е достапен.");
    }

    /** Бајтите на сликата по случајниот клуч — ова го служи отворената рута. */
    public AvatarData avatarByKey(String key) {
        ProfileImage image = profileImageRepository.findByAvatarKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Сликата не постои."));

        return new AvatarData(image.getContentType(), image.getData());
    }

    // ---- Промена ----------------------------------------------------------

    @Transactional
    public ProfileResponse updateName(User current, UpdateProfileRequest request) {
        User user = loadWithInterests(current.getId());
        user.setFullName(request.fullName().trim());
        userRepository.save(user);

        log.info("Корисник {} го смени името", user.getId());

        return mapper.toProfileResponse(user, user.getEmail(), interestsOf(user), statsOf(user));
    }

    @Transactional
    public ProfileResponse uploadAvatar(User current, MultipartFile file) {
        byte[] processed = process(file);

        User user = loadWithInterests(current.getId());

        user.setAvatarKey(newAvatarKey());
        userRepository.save(user);

        ProfileImage image = profileImageRepository.findById(user.getId())
                .orElseGet(() -> ProfileImage.builder().user(user).build());

        image.setContentType(STORED_CONTENT_TYPE);
        image.setData(processed);
        image.setUpdatedAt(Instant.now());
        profileImageRepository.save(image);

        log.info("Корисник {} качи профилна слика ({} bytes)", user.getId(), processed.length);

        return mapper.toProfileResponse(user, user.getEmail(), interestsOf(user), statsOf(user));
    }

    @Transactional
    public ProfileResponse removeAvatar(User current) {
        User user = loadWithInterests(current.getId());

        user.setAvatarKey(null);
        userRepository.save(user);
        profileImageRepository.deleteByUserId(user.getId());

        return mapper.toProfileResponse(user, user.getEmail(), interestsOf(user), statsOf(user));
    }

    // ---- Обработка на сликата ---------------------------------------------

    private byte[] process(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Не е избрана слика.");
        }

        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Сликата е поголема од 2 MB.");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Дозволени формати: PNG, JPG, WEBP или GIF.");
        }

        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (source == null) {
                throw new IllegalArgumentException("Датотеката не е валидна слика.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(squareThumbnail(source), "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            log.warn("Не успеа обработка на профилна слика: {}", e.getMessage());
            throw new IllegalArgumentException("Сликата не можеше да се прочита.");
        }
    }

    private BufferedImage squareThumbnail(BufferedImage source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        BufferedImage cropped = source.getSubimage(x, y, side, side);

        int target = Math.min(MAX_SIDE, side);
        BufferedImage output = new BufferedImage(target, target, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, target, target);
            graphics.drawImage(cropped, 0, 0, target, target, null);
        } finally {
            graphics.dispose();
        }

        return output;
    }

    private String newAvatarKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ---- Помошни ----------------------------------------------------------

    private ProfileStats statsOf(User user) {
        return switch (user.getRole()) {
            case MENTOR -> new ProfileStats(
                    sessionRepository.countByMentorId(user.getId()),
                    sessionRepository.countUpcomingByMentorId(user.getId(), LocalDateTime.now()),
                    0,
                    0
            );
            case STUDENT -> new ProfileStats(
                    0,
                    0,
                    applicationRepository.countByStudent(user),
                    applicationRepository.countByStudentAndStatus(user, ApplicationStatus.ACCEPTED)
            );
            case ADMIN -> ProfileStats.EMPTY;
        };
    }

    private List<SubjectResponse> interestsOf(User user) {
        if (user.getRole() != Role.STUDENT) {
            return List.of();
        }

        return user.getInterests().stream()
                .map(mapper::toSubjectResponse)
                .sorted(Comparator.comparing(SubjectResponse::name))
                .toList();
    }

    private User loadWithInterests(Long userId) {
        return userRepository.findWithInterestsById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Не постои корисник со ID " + userId));
    }
}
