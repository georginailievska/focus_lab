package mk.focuslab.service;

import mk.focuslab.dto.ProfileResponse;
import mk.focuslab.dto.UpdateProfileRequest;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.ProfileImage;
import mk.focuslab.model.Role;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.ProfileImageRepository;
import mk.focuslab.repository.SessionApplicationRepository;
import mk.focuslab.repository.SessionRepository;
import mk.focuslab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileImageRepository profileImageRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionApplicationRepository applicationRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                userRepository, profileImageRepository, sessionRepository,
                applicationRepository, new DtoMapper());
    }

    private User student(Long id) {
        Set<Subject> interests = new HashSet<>();
        interests.add(subject(1L, "Бази на податоци"));
        interests.add(subject(2L, "Алгоритми"));

        return User.builder()
                .id(id)
                .fullName("Ана Николоска")
                .email("ana@students.finki.ukim.mk")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .interests(interests)
                .build();
    }

    private User mentor(Long id) {
        return User.builder()
                .id(id)
                .fullName("Марко Стоев")
                .email("marko@finki.ukim.mk")
                .passwordHash("hash")
                .role(Role.MENTOR)
                .avatarKey("kluc-123")
                .interests(new HashSet<>())
                .build();
    }

    private Subject subject(Long id, String name) {
        return Subject.builder().id(id).name(name).build();
    }

    @Test
    @DisplayName("Мојот профил носи email и интереси")
    void ownProfileIncludesPrivateFields() {
        User user = student(7L);
        when(userRepository.findWithInterestsById(7L)).thenReturn(Optional.of(user));

        ProfileResponse profile = profileService.myProfile(user);

        assertThat(profile.email()).isEqualTo("ana@students.finki.ukim.mk");
        assertThat(profile.interests()).extracting("name")
                .containsExactly("Алгоритми", "Бази на податоци"); // сортирани по име
    }

    @Test
    @DisplayName("Профил на друг корисник е без email и без интереси")
    void otherProfileHidesPrivateFields() {
        User viewer = student(7L);
        User target = mentor(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(target));

        ProfileResponse profile = profileService.profileOf(9L, viewer);

        assertThat(profile.fullName()).isEqualTo("Марко Стоев");
        assertThat(profile.email()).isNull();
        assertThat(profile.interests()).isNull();
        // сликата е јавна по дизајн — токму затоа патеката е случаен клуч
        assertThat(profile.avatarUrl()).isEqualTo("/api/avatars/kluc-123");
    }

    @Test
    @DisplayName("Студент не може да отвори профил на друг студент")
    void studentCannotOpenAnotherStudent() {
        User viewer = student(7L);
        User other = student(8L);
        when(userRepository.findById(8L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> profileService.profileOf(8L, viewer))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Менторот смее да отвори профил на студент")
    void mentorCanOpenStudent() {
        User viewer = User.builder().id(3L).fullName("Ментор").email("m@finki.ukim.mk")
                .passwordHash("hash").role(Role.MENTOR).interests(new HashSet<>()).build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(student(8L)));

        assertThat(profileService.profileOf(8L, viewer).fullName()).isNotBlank();
    }

    @Test
    @DisplayName("Администраторот го гледа email-от на другите профили")
    void adminSeesEmail() {
        User admin = User.builder().id(1L).fullName("Админ").email("admin@focuslab.mk")
                .passwordHash("hash").role(Role.ADMIN).interests(new HashSet<>()).build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(mentor(9L)));

        assertThat(profileService.profileOf(9L, admin).email()).isEqualTo("marko@finki.ukim.mk");
    }

    @Test
    @DisplayName("Сопствениот профил преку /users/{id} си е сопствен профил")
    void ownProfileThroughPublicRoute() {
        User user = student(7L);
        when(userRepository.findWithInterestsById(7L)).thenReturn(Optional.of(user));

        assertThat(profileService.profileOf(7L, user).email()).isEqualTo("ana@students.finki.ukim.mk");
    }

    @Test
    @DisplayName("Името се сечат празните места")
    void updateNameTrims() {
        User user = student(7L);
        when(userRepository.findWithInterestsById(7L)).thenReturn(Optional.of(user));

        ProfileResponse profile = profileService.updateName(user, new UpdateProfileRequest("  Ана М. Николоска  "));

        assertThat(profile.fullName()).isEqualTo("Ана М. Николоска");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Качената слика се сече на квадрат и се пре-кодира во JPEG")
    void uploadAvatarProcessesImage() throws IOException {
        User user = student(7L);
        when(userRepository.findWithInterestsById(7L)).thenReturn(Optional.of(user));
        when(profileImageRepository.findById(7L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "slika.png", "image/png", pngOf(800, 400));

        ProfileResponse profile = profileService.uploadAvatar(user, file);

        ArgumentCaptor<ProfileImage> saved = ArgumentCaptor.forClass(ProfileImage.class);
        verify(profileImageRepository).save(saved.capture());

        assertThat(saved.getValue().getContentType()).isEqualTo("image/jpeg");

        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(saved.getValue().getData()));
        assertThat(stored.getWidth()).isEqualTo(stored.getHeight());
        assertThat(stored.getWidth()).isEqualTo(400); // помалата страна, под лимитот од 512

        // Нов клуч при секое качување → старата патека веднаш престанува да важи
        assertThat(user.getAvatarKey()).isNotBlank();
        assertThat(profile.avatarUrl()).isEqualTo("/api/avatars/" + user.getAvatarKey());
    }

    @Test
    @DisplayName("Датотека што не е слика се одбива, иако тврди дека е PNG")
    void uploadAvatarRejectsNonImageBytes() {
        User user = student(7L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> profileService.uploadAvatar(user, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не е валидна слика");

        verify(profileImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Недозволен формат се одбива")
    void uploadAvatarRejectsForeignContentType() {
        User user = student(7L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes());

        assertThatThrownBy(() -> profileService.uploadAvatar(user, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Дозволени формати");
    }

    @Test
    @DisplayName("Празна датотека се одбива")
    void uploadAvatarRejectsEmptyFile() {
        User user = student(7L);
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> profileService.uploadAvatar(user, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Не е избрана слика");
    }

    @Test
    @DisplayName("Отстранување на слика го брише и клучот и записот")
    void removeAvatarClearsBoth() {
        User user = mentor(9L);
        when(userRepository.findWithInterestsById(9L)).thenReturn(Optional.of(user));

        ProfileResponse profile = profileService.removeAvatar(user);

        assertThat(user.getAvatarKey()).isNull();
        assertThat(profile.avatarUrl()).isNull();
        verify(profileImageRepository).deleteByUserId(9L);
    }

    @Test
    @DisplayName("Непознат клуч за слика враќа 404, не празна слика")
    void unknownAvatarKeyIsNotFound() {
        when(profileImageRepository.findByAvatarKey("nema")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.avatarByKey("nema"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** Вистински PNG бајти — за да има што ImageIO да декодира. */
    private byte[] pngOf(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
