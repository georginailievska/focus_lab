package mk.focuslab.service;

import mk.focuslab.event.ApplicationDecidedEvent;
import mk.focuslab.event.ApplicationReceivedEvent;
import mk.focuslab.event.Recipient;
import mk.focuslab.event.SessionCancelledEvent;
import mk.focuslab.event.SessionCreatedEvent;
import mk.focuslab.event.SessionUpdatedEvent;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.Notification;
import mk.focuslab.model.NotificationType;
import mk.focuslab.model.Role;
import mk.focuslab.model.Session;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.NotificationRepository;
import mk.focuslab.repository.SessionRepository;
import mk.focuslab.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Што влиза во ѕвончето и кому. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private NotificationService notificationService;

    private User student(long id) {
        return User.builder()
                .id(id)
                .fullName("Студент " + id)
                .email("student" + id + "@students.finki.ukim.mk")
                .passwordHash("x")
                .role(Role.STUDENT)
                .build();
    }

    private User mentor(long id) {
        return User.builder()
                .id(id)
                .fullName("Ментор " + id)
                .email("mentor" + id + "@finki.ukim.mk")
                .passwordHash("x")
                .role(Role.MENTOR)
                .build();
    }

    private Session session() {
        return Session.builder()
                .id(7L)
                .title("Вежби по бази")
                .subject(Subject.builder().id(3L).name("Бази на податоци").build())
                .startTime(LocalDateTime.of(2026, 12, 1, 10, 0))
                .endTime(LocalDateTime.of(2026, 12, 1, 12, 0))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Notification> savedBatch() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Notification savedOne() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Нова сесија стигнува до сите со интерес за предметот")
    void newSessionReachesInterestedStudents() {
        Session session = session();
        when(sessionRepository.findWithSubjectById(7L)).thenReturn(Optional.of(session));
        when(userRepository.findInterestedInSubject(3L, Role.STUDENT))
                .thenReturn(List.of(student(1L), student(2L)));

        notificationService.onSessionCreated(new SessionCreatedEvent(7L));

        List<Notification> saved = savedBatch();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.NEW_SESSION);
            assertThat(notification.getTitle()).contains("Бази на податоци");
            assertThat(notification.getSessionId()).isEqualTo(7L);
            assertThat(notification.getReadAt()).isNull();
        });
    }

    @Test
    @DisplayName("Нема кому — нема ниту еден ред")
    void newSessionWithoutInterestedStudentsSavesNothing() {
        when(sessionRepository.findWithSubjectById(7L)).thenReturn(Optional.of(session()));
        when(userRepository.findInterestedInSubject(anyLong(), any())).thenReturn(List.of());

        notificationService.onSessionCreated(new SessionCreatedEvent(7L));

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Избришана сесија не прави известување")
    void missingSessionIsSkipped() {
        when(sessionRepository.findWithSubjectById(7L)).thenReturn(Optional.empty());

        notificationService.onSessionCreated(new SessionCreatedEvent(7L));

        verify(notificationRepository, never()).saveAll(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Примена пријава — известување само за тој студент")
    void applicationReceivedGoesToTheStudent() {
        User student = student(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(student));

        notificationService.onApplicationReceived(new ApplicationReceivedEvent(
                4L, student.getEmail(), student.getFullName(), 7L, "Вежби по бази"));

        Notification saved = savedOne();
        assertThat(saved.getRecipient()).isSameAs(student);
        assertThat(saved.getType()).isEqualTo(NotificationType.APPLICATION_RECEIVED);
        assertThat(saved.getSessionId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Истата пријава стигнува и до менторите на сесијата")
    void applicationReceivedAlsoReachesMentors() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(student(4L)));
        when(sessionRepository.findMentorsBySessionId(7L))
                .thenReturn(List.of(mentor(11L), mentor(12L)));

        notificationService.onApplicationReceived(new ApplicationReceivedEvent(
                4L, "x@y", "Ана Николоска", 7L, "Вежби по бази"));

        List<Notification> saved = savedBatch();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.NEW_APPLICATION);
            assertThat(notification.getBody()).contains("Ана Николоска");
            assertThat(notification.getSessionId()).isEqualTo(7L);
        });
    }

    @Test
    @DisplayName("Прифатена и одбиена пријава се разликуваат по вид")
    void decisionUsesItsOwnType() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(student(4L)));

        notificationService.onApplicationDecided(new ApplicationDecidedEvent(
                4L, "x@y", "Студент", 7L, "Вежби по бази",
                LocalDateTime.of(2026, 12, 1, 10, 0), true));

        assertThat(savedOne().getType()).isEqualTo(NotificationType.APPLICATION_ACCEPTED);
    }

    @Test
    @DisplayName("Одбиената пријава не води кон сесијата што ја нема")
    void rejectedDecisionIsMarkedAsRejected() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(student(4L)));

        notificationService.onApplicationDecided(new ApplicationDecidedEvent(
                4L, "x@y", "Студент", 7L, "Вежби по бази",
                LocalDateTime.of(2026, 12, 1, 10, 0), false));

        assertThat(savedOne().getType()).isEqualTo(NotificationType.APPLICATION_REJECTED);
    }

    @Test
    @DisplayName("Корисник што го нема не крши ништо")
    void unknownRecipientIsSkipped() {
        when(userRepository.findById(4L)).thenReturn(Optional.empty());

        notificationService.onApplicationReceived(new ApplicationReceivedEvent(
                4L, "x@y", "Студент", 7L, "Вежби по бази"));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Променета сесија стигнува до сите пријавени, со линк до сесијата")
    void updatedSessionReachesRecipients() {
        when(userRepository.getReferenceById(anyLong())).thenReturn(student(1L));

        notificationService.onSessionUpdated(new SessionUpdatedEvent(
                7L, "Вежби по бази", List.of("Ново време: 02.12.2026 10:00"),
                List.of(new Recipient(1L, "a@b", "Ана"), new Recipient(2L, "c@d", "Бојан"))));

        List<Notification> saved = savedBatch();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getType()).isEqualTo(NotificationType.SESSION_UPDATED);
        assertThat(saved.get(0).getBody()).contains("Ново време");
        assertThat(saved.get(0).getSessionId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Откажана сесија нема линк — сесијата ја нема веќе")
    void cancelledSessionHasNoLink() {
        when(userRepository.getReferenceById(anyLong())).thenReturn(student(1L));

        notificationService.onSessionCancelled(new SessionCancelledEvent(
                "Вежби по бази", LocalDateTime.of(2026, 12, 1, 10, 0),
                List.of(new Recipient(1L, "a@b", "Ана"))));

        Notification saved = savedBatch().get(0);
        assertThat(saved.getType()).isEqualTo(NotificationType.SESSION_CANCELLED);
        assertThat(saved.getSessionId()).isNull();
    }

    @Test
    @DisplayName("Долг наслов на сесија не ја крши колоната")
    void longTextIsCut() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(student(4L)));

        notificationService.onApplicationReceived(new ApplicationReceivedEvent(
                4L, "x@y", "Студент", 7L, "н".repeat(900)));

        assertThat(savedOne().getBody().length()).isLessThanOrEqualTo(Notification.MAX_BODY_LENGTH);
    }

    @Test
    @DisplayName("Означувањето прочитано важи само за своите редови")
    void markReadIsScopedToTheOwner() {
        notificationService.markRead(55L, student(4L));

        verify(notificationRepository).markRead(eq(55L), eq(4L), any(Instant.class));
    }

    @Test
    @DisplayName("Бројот на непрочитани е по корисник")
    void unreadCountIsPerUser() {
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(4L)).thenReturn(3L);

        assertThat(notificationService.unreadCount(student(4L))).isEqualTo(3L);
    }
}
