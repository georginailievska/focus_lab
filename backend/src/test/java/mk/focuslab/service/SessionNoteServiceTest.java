package mk.focuslab.service;

import mk.focuslab.dto.SessionNoteRequest;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.Session;
import mk.focuslab.model.SessionNote;
import mk.focuslab.model.User;
import mk.focuslab.repository.SessionNoteRepository;
import mk.focuslab.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Правата на забелешките: кој чита, кој пишува и кој брише. */
@ExtendWith(MockitoExtension.class)
class SessionNoteServiceTest {
    @Mock
    private SessionNoteRepository noteRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private SessionNoteService noteService;

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

    private User student() {
        return User.builder()
                .id(9L)
                .fullName("Ана Николоска")
                .email("ana@students.finki.ukim.mk")
                .passwordHash("x")
                .role(Role.STUDENT)
                .build();
    }

    private SessionNote note(long id, User author) {
        return SessionNote.builder().id(id).author(author).text("Дојдоа 8 од 12").build();
    }

    @Test
    @DisplayName("Студент не смее ни да ги прочита забелешките")
    void studentCannotRead() {
        assertThatThrownBy(() -> noteService.listNotes(1L, student()))
                .isInstanceOf(ForbiddenActionException.class);

        verify(noteRepository, never()).findBySessionIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Неодобрен ментор не смее да пишува")
    void pendingMentorCannotWrite() {
        assertThatThrownBy(() -> noteService.addNote(
                1L, new SessionNoteRequest("текст"), mentor(2L, MentorStatus.PENDING)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ментор што не ја води сесијата сепак пишува забелешка")
    void anyApprovedMentorCanWrite() {
        User outsider = mentor(3L, MentorStatus.APPROVED);
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(new Session()));
        when(noteRepository.save(any(SessionNote.class))).thenAnswer(call -> call.getArgument(0));

        noteService.addNote(7L, new SessionNoteRequest("  Третата задача им беше најтешка  "), outsider);

        ArgumentCaptor<SessionNote> saved = ArgumentCaptor.forClass(SessionNote.class);
        verify(noteRepository).save(saved.capture());

        assertThat(saved.getValue().getAuthor()).isSameAs(outsider);
        assertThat(saved.getValue().getText()).isEqualTo("Третата задача им беше најтешка");
    }

    @Test
    @DisplayName("Забелешка на непостоечка сесија е 404")
    void missingSessionIsNotFound() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.addNote(
                7L, new SessionNoteRequest("текст"), mentor(2L, MentorStatus.APPROVED)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Секој ментор брише само свои забелешки")
    void onlyAuthorDeletes() {
        User author = mentor(2L, MentorStatus.APPROVED);
        User other = mentor(3L, MentorStatus.APPROVED);
        when(noteRepository.findWithAuthorById(4L)).thenReturn(Optional.of(note(4L, author)));

        assertThatThrownBy(() -> noteService.deleteNote(4L, other))
                .isInstanceOf(ForbiddenActionException.class);

        verify(noteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Авторот ја брише својата забелешка")
    void authorDeletes() {
        User author = mentor(2L, MentorStatus.APPROVED);
        SessionNote existing = note(4L, author);
        when(noteRepository.findWithAuthorById(4L)).thenReturn(Optional.of(existing));

        noteService.deleteNote(4L, author);

        verify(noteRepository).delete(existing);
    }
}
