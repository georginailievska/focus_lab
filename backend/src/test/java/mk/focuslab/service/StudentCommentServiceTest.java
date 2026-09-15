package mk.focuslab.service;

import mk.focuslab.dto.StudentCommentRequest;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.StudentComment;
import mk.focuslab.model.User;
import mk.focuslab.repository.StudentCommentRepository;
import mk.focuslab.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Кој смее да коментира, на кого, и кој ја носи видливоста. */
@ExtendWith(MockitoExtension.class)
class StudentCommentServiceTest {
    @Mock
    private StudentCommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private StudentCommentService commentService;

    private User mentor(long id, MentorStatus status) {
        return User.builder()
                .id(id)
                .fullName("Ментор " + id)
                .email("mentor" + id + "@finki.ukim.mk")
                .passwordHash("x")
                .role(Role.MENTOR)
                .mentorStatus(status)
                .build();
    }

    private User student(long id) {
        return User.builder()
                .id(id)
                .fullName("Ана Николоска")
                .email("ana" + id + "@students.finki.ukim.mk")
                .passwordHash("x")
                .role(Role.STUDENT)
                .build();
    }

    private StudentComment comment(long id, User author, boolean shared) {
        return StudentComment.builder()
                .id(id)
                .author(author)
                .text("Добро подготвена, вреди да се повика повторно")
                .sharedWithMentors(shared)
                .build();
    }

    @Test
    @DisplayName("Студент не смее да чита коментари за друг студент")
    void studentCannotRead() {
        assertThatThrownBy(() -> commentService.listComments(5L, student(9L)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(commentRepository, never()).findVisibleForStudent(any(), any());
    }

    @Test
    @DisplayName("Неодобрен ментор не смее да коментира")
    void pendingMentorCannotWrite() {
        assertThatThrownBy(() -> commentService.addComment(
                5L, new StudentCommentRequest("текст", true), mentor(2L, MentorStatus.PENDING)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Коментар на профил на ментор не е дозволен")
    void cannotCommentOnMentor() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(mentor(4L, MentorStatus.APPROVED)));

        assertThatThrownBy(() -> commentService.addComment(
                4L, new StudentCommentRequest("текст", true), mentor(2L, MentorStatus.APPROVED)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Непостоечки корисник е 404")
    void missingUserIsNotFound() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.listComments(5L, mentor(2L, MentorStatus.APPROVED)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Видливоста од барањето се зачувува како што е избрана")
    void savesChosenVisibility() {
        User author = mentor(2L, MentorStatus.APPROVED);
        when(userRepository.findById(5L)).thenReturn(Optional.of(student(5L)));
        when(commentRepository.save(any(StudentComment.class))).thenAnswer(call -> call.getArgument(0));

        commentService.addComment(5L, new StudentCommentRequest("  приватна белешка  ", false), author);

        ArgumentCaptor<StudentComment> saved = ArgumentCaptor.forClass(StudentComment.class);
        verify(commentRepository).save(saved.capture());

        assertThat(saved.getValue().isSharedWithMentors()).isFalse();
        assertThat(saved.getValue().getText()).isEqualTo("приватна белешка");
        assertThat(saved.getValue().getAuthor()).isSameAs(author);
    }

    @Test
    @DisplayName("Читањето ги бара само видливите за тој ментор")
    void readingPassesViewerId() {
        User viewer = mentor(2L, MentorStatus.APPROVED);
        when(userRepository.findById(5L)).thenReturn(Optional.of(student(5L)));
        when(commentRepository.findVisibleForStudent(5L, 2L)).thenReturn(List.of());

        commentService.listComments(5L, viewer);

        verify(commentRepository).findVisibleForStudent(5L, 2L);
    }

    @Test
    @DisplayName("Видливоста и бришењето ги менува само авторот")
    void onlyAuthorChanges() {
        User author = mentor(2L, MentorStatus.APPROVED);
        User other = mentor(3L, MentorStatus.APPROVED);
        when(commentRepository.findWithAuthorById(anyLong()))
                .thenReturn(Optional.of(comment(4L, author, false)));

        assertThatThrownBy(() -> commentService.changeVisibility(4L, true, other))
                .isInstanceOf(ForbiddenActionException.class);
        assertThatThrownBy(() -> commentService.deleteComment(4L, other))
                .isInstanceOf(ForbiddenActionException.class);

        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Авторот ја менува видливоста на својот коментар")
    void authorChangesVisibility() {
        User author = mentor(2L, MentorStatus.APPROVED);
        StudentComment existing = comment(4L, author, false);
        when(commentRepository.findWithAuthorById(4L)).thenReturn(Optional.of(existing));

        commentService.changeVisibility(4L, true, author);

        assertThat(existing.isSharedWithMentors()).isTrue();
        verify(commentRepository).save(existing);
    }
}
