package mk.focuslab.service;

import mk.focuslab.dto.CommentRequest;
import mk.focuslab.dto.PostRequest;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.Post;
import mk.focuslab.model.PostComment;
import mk.focuslab.model.Role;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.PostAttachmentRepository;
import mk.focuslab.repository.PostCommentRepository;
import mk.focuslab.repository.PostRepository;
import mk.focuslab.repository.SubjectRepository;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostCommentRepository commentRepository;
    @Mock
    private PostAttachmentRepository attachmentRepository;
    @Mock
    private SubjectRepository subjectRepository;

    private FeedService feedService;

    @BeforeEach
    void setUp() {
        feedService = new FeedService(
                postRepository, commentRepository, attachmentRepository,
                subjectRepository, new DtoMapper());
    }

    private User user(long id, Role role) {
        return User.builder()
                .id(id)
                .fullName("Корисник " + id)
                .email("user" + id + "@students.finki.ukim.mk")
                .passwordHash("x")
                .role(role)
                .build();
    }

    private Post post(User author) {
        return Post.builder()
                .id(1L)
                .author(author)
                .text("Не ми оди рекурзијата во втората задача.")
                .attachments(new ArrayList<>())
                .comments(new ArrayList<>())
                .build();
    }

    private byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 40, 30);
        graphics.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private PostRequest request() {
        return new PostRequest("  Помош со втората задача?  ", null);
    }

    @Test
    @DisplayName("Објава со слика: текстот е тримуван, прилогот добива случаен клуч")
    void createsPostWithImage() throws IOException {
        User student = user(5L, Role.STUDENT);
        MockMultipartFile file = new MockMultipartFile("files", "zadaca.png", "image/png", png());

        feedService.createPost(student, request(), List.of(file));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(saved.capture());

        Post created = saved.getValue();
        assertThat(created.getText()).isEqualTo("Помош со втората задача?");
        assertThat(created.getAttachments()).hasSize(1);

        var attachment = created.getAttachments().get(0);
        assertThat(attachment.isImage()).isTrue();
        assertThat(attachment.getFilename()).isEqualTo("zadaca.png");
        assertThat(attachment.getAccessKey()).isNotBlank().hasSizeGreaterThan(20);
        assertThat(attachment.getBlob().getData()).isEqualTo(file.getBytes());
    }

    @Test
    @DisplayName("Датотека што тврди дека е слика, а не е, се одбива")
    void rejectsFakeImage() {
        User student = user(5L, Role.STUDENT);
        MockMultipartFile file = new MockMultipartFile(
                "files", "zadaca.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> feedService.createPost(student, request(), List.of(file)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не е валидна слика");

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("SVG не е дозволен — се извршува во прелистувачот")
    void rejectsSvg() {
        User student = user(5L, Role.STUDENT);
        MockMultipartFile file = new MockMultipartFile(
                "files", "zadaca.svg", "image/svg+xml", "<svg onload=\"alert(1)\"/>".getBytes());

        assertThatThrownBy(() -> feedService.createPost(student, request(), List.of(file)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не е дозволен");
    }

    @Test
    @DisplayName("Повеќе од четири датотеки се одбиваат")
    void rejectsTooManyFiles() throws IOException {
        User student = user(5L, Role.STUDENT);
        byte[] bytes = png();

        List<MockMultipartFile> files = List.of(
                new MockMultipartFile("files", "1.png", "image/png", bytes),
                new MockMultipartFile("files", "2.png", "image/png", bytes),
                new MockMultipartFile("files", "3.png", "image/png", bytes),
                new MockMultipartFile("files", "4.png", "image/png", bytes),
                new MockMultipartFile("files", "5.png", "image/png", bytes));

        assertThatThrownBy(() -> feedService.createPost(student, request(), List.copyOf(files)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Најмногу 4");
    }

    @Test
    @DisplayName("Патеката од прелистувачот се отсекува — останува само името")
    void stripsPathFromFilename() throws IOException {
        User student = user(5L, Role.STUDENT);
        MockMultipartFile file = new MockMultipartFile(
                "files", "C:\\Users\\ana\\Desktop\\zadaca.pdf", "application/pdf", png());

        feedService.createPost(student, request(), List.of(file));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(saved.capture());

        var attachment = saved.getValue().getAttachments().get(0);
        assertThat(attachment.getFilename()).isEqualTo("zadaca.pdf");
        // PDF не е слика → се симнува, не се прикажува во страницата
        assertThat(attachment.isImage()).isFalse();
    }

    @Test
    @DisplayName("Админот не пишува во лентата — само модерира")
    void adminCannotPost() {
        assertThatThrownBy(() -> feedService.createPost(user(1L, Role.ADMIN), request(), List.of()))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Ментор коментира на објава на студент")
    void mentorCanComment() {
        User student = user(5L, Role.STUDENT);
        User mentor = user(3L, Role.MENTOR);
        Post existing = post(student);

        when(postRepository.findWithAuthorById(1L)).thenReturn(Optional.of(existing));

        feedService.addComment(1L, mentor, new CommentRequest("  Пробај со мемоизација.  "));

        ArgumentCaptor<PostComment> saved = ArgumentCaptor.forClass(PostComment.class);
        verify(commentRepository).save(saved.capture());

        assertThat(saved.getValue().getText()).isEqualTo("Пробај со мемоизација.");
        assertThat(saved.getValue().getAuthor()).isEqualTo(mentor);
        assertThat(existing.getComments()).hasSize(1);
    }

    @Test
    @DisplayName("Коментар на непостоечка објава е 404")
    void commentOnMissingPost() {
        when(postRepository.findWithAuthorById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.addComment(99L, user(5L, Role.STUDENT), new CommentRequest("?")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Туѓа објава не се брише")
    void cannotDeleteForeignPost() {
        when(postRepository.findWithAuthorById(1L)).thenReturn(Optional.of(post(user(5L, Role.STUDENT))));

        assertThatThrownBy(() -> feedService.deletePost(1L, user(6L, Role.STUDENT)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Авторот ја брише својата објава")
    void authorDeletesOwnPost() {
        User student = user(5L, Role.STUDENT);
        Post existing = post(student);
        when(postRepository.findWithAuthorById(1L)).thenReturn(Optional.of(existing));

        feedService.deletePost(1L, student);

        verify(postRepository).delete(existing);
    }

    @Test
    @DisplayName("Админот може да избрише туѓа објава — модерација")
    void adminDeletesAnyPost() {
        Post existing = post(user(5L, Role.STUDENT));
        when(postRepository.findWithAuthorById(1L)).thenReturn(Optional.of(existing));

        feedService.deletePost(1L, user(1L, Role.ADMIN));

        verify(postRepository).delete(existing);
    }

    @Test
    @DisplayName("Објава со предмет го поставува предметот")
    void createsPostWithSubject() {
        User student = user(5L, Role.STUDENT);
        when(subjectRepository.findById(2L))
                .thenReturn(Optional.of(Subject.builder().id(2L).name("Databases").build()));

        feedService.createPost(student, new PostRequest("Задача 3?", 2L), null);

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(saved.capture());

        assertThat(saved.getValue().getSubject().getName()).isEqualTo("Databases");
    }
}
