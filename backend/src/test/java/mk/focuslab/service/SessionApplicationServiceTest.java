package mk.focuslab.service;

import mk.focuslab.dto.DecisionRequest;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.SessionFullException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.ApplicationStatus;
import mk.focuslab.model.BusinessRules;
import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.Session;
import mk.focuslab.model.SessionApplication;
import mk.focuslab.model.SessionMode;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.SessionApplicationRepository;
import mk.focuslab.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
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
class SessionApplicationServiceTest {
    @Mock
    private SessionApplicationRepository applicationRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private SessionApplicationService service;

    private User user(long id, Role role) {
        return User.builder()
                .id(id)
                .fullName((role == Role.MENTOR ? "Ментор " : "Корисник ") + id)
                .email("user" + id + "@focuslab.mk")
                .passwordHash("x")
                .role(role)
                .mentorStatus(role == Role.MENTOR ? MentorStatus.APPROVED : null)
                .build();
    }

    private Session sessionWithMentor(User mentor) {
        Set<User> mentors = new HashSet<>();
        mentors.add(mentor);

        return Session.builder()
                .id(7L)
                .title("Сесија")
                .subject(Subject.builder().id(1L).name("Databases").build())
                .mentors(mentors)
                .mode(SessionMode.ONLINE)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();
    }

    private SessionApplication application(Session session, User student) {
        return SessionApplication.builder()
                .id(11L)
                .session(session)
                .student(student)
                .status(ApplicationStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("PENDING не е валидна одлука")
    void rejectsPendingAsDecision() {
        assertThatThrownBy(() -> service.decide(
                11L, new DecisionRequest(ApplicationStatus.PENDING), user(1L, Role.MENTOR)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(applicationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Ментор што не е на таа сесија не смее да одлучува")
    void rejectsMentorOfAnotherSession() {
        User sessionMentor = user(1L, Role.MENTOR);
        User otherMentor = user(9L, Role.MENTOR);
        Session session = sessionWithMentor(sessionMentor);

        when(applicationRepository.findById(11L))
                .thenReturn(Optional.of(application(session, user(5L, Role.STUDENT))));

        assertThatThrownBy(() -> service.decide(
                11L, new DecisionRequest(ApplicationStatus.ACCEPTED), otherMentor))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Admin смее да одлучува за секоја пријава")
    void allowsAdmin() {
        Session session = sessionWithMentor(user(1L, Role.MENTOR));
        SessionApplication app = application(session, user(5L, Role.STUDENT));

        when(applicationRepository.findById(11L)).thenReturn(Optional.of(app));
        when(sessionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(session));
        when(applicationRepository.countBySessionAndStatus(session, ApplicationStatus.ACCEPTED)).thenReturn(3L);

        service.decide(11L, new DecisionRequest(ApplicationStatus.ACCEPTED), user(2L, Role.ADMIN));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(app.getDecidedAt()).isNotNull();
        verify(applicationRepository).save(app);
        verify(events).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Одобрувањето се одбива кога лимитот на одобрени е достигнат")
    void rejectsAcceptWhenApprovedLimitReached() {
        User mentor = user(1L, Role.MENTOR);
        Session session = sessionWithMentor(mentor);
        SessionApplication app = application(session, user(5L, Role.STUDENT));

        when(applicationRepository.findById(11L)).thenReturn(Optional.of(app));
        when(sessionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(session));
        when(applicationRepository.countBySessionAndStatus(session, ApplicationStatus.ACCEPTED))
                .thenReturn((long) BusinessRules.DEFAULT_MAX_APPROVED);

        assertThatThrownBy(() -> service.decide(
                11L, new DecisionRequest(ApplicationStatus.ACCEPTED), mentor))
                .isInstanceOf(SessionFullException.class);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Одбивањето не е ограничено со лимитот на одобрени")
    void allowsRejectEvenWhenFull() {
        User mentor = user(1L, Role.MENTOR);
        Session session = sessionWithMentor(mentor);
        SessionApplication app = application(session, user(5L, Role.STUDENT));

        when(applicationRepository.findById(11L)).thenReturn(Optional.of(app));
        when(sessionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(session));

        service.decide(11L, new DecisionRequest(ApplicationStatus.REJECTED), mentor);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(applicationRepository, never()).countBySessionAndStatus(any(), any());
        verify(applicationRepository).save(app);
    }
}
