package mk.focuslab.service;

import mk.focuslab.dto.SessionRequest;
import mk.focuslab.exception.DuplicateApplicationException;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.event.Recipient;
import mk.focuslab.event.SessionCancelledEvent;
import mk.focuslab.event.SessionUpdatedEvent;
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
import mk.focuslab.repository.SessionNoteRepository;
import mk.focuslab.repository.SessionRepository;
import mk.focuslab.repository.SubjectRepository;
import mk.focuslab.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionApplicationRepository applicationRepository;
    @Mock
    private SessionNoteRepository noteRepository;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private SessionService sessionService;

    @Captor
    private ArgumentCaptor<Session> savedSession;

    // ------------------------------------------------------------ помошни

    private static final LocalDateTime TOMORROW_10 = LocalDateTime.now().plusDays(1)
            .withHour(10).withMinute(0).withSecond(0).withNano(0);

    private User mentor(long id, MentorStatus status) {
        return User.builder()
                .id(id)
                .fullName("Ментор " + id)
                .email("mentor" + id + "@focuslab.mk")
                .passwordHash("x")
                .role(Role.MENTOR)
                .mentorStatus(status)
                .build();
    }

    private User student(long id) {
        return User.builder()
                .id(id)
                .fullName("Студент " + id)
                .email("student" + id + "@focuslab.mk")
                .passwordHash("x")
                .role(Role.STUDENT)
                .build();
    }

    private SessionRequest request(LocalDateTime start, LocalDateTime end, SessionMode mode, String location) {
        return new SessionRequest(
                "  Подготовка за колоквиум  ", "опис", 1L, List.of(),
                mode, location, start, end, Set.of("  колоквиум  ", "  ", "алгоритми")
        );
    }

    private SessionApplication application(Session session, User student, ApplicationStatus status) {
        return SessionApplication.builder()
                .id(11L)
                .session(session)
                .student(student)
                .status(status)
                .build();
    }

    private Session session(LocalDateTime start, LocalDateTime end) {
        return Session.builder()
                .id(7L)
                .title("Сесија")
                .subject(Subject.builder().id(1L).name("Databases").build())
                .mode(SessionMode.ONLINE)
                .startTime(start)
                .endTime(end)
                .build();
    }

    // -------------------------------------------------- createSession: правила

    @Test
    @DisplayName("Ментор што не е одобрен не може да закажува сесии")
    void rejectsUnapprovedMentor() {
        SessionRequest request = request(TOMORROW_10, TOMORROW_10.plusHours(2), SessionMode.ONLINE, null);

        assertThatThrownBy(() -> sessionService.createSession(request, mentor(1L, MentorStatus.PENDING)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Крајот не смее да биде пред почетокот")
    void rejectsEndBeforeStart() {
        SessionRequest request = request(TOMORROW_10, TOMORROW_10.minusHours(1), SessionMode.ONLINE, null);

        assertThatThrownBy(() -> sessionService.createSession(request, mentor(1L, MentorStatus.APPROVED)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Крајот не смее да биде на друг ден од почетокот")
    void rejectsEndOnAnotherDay() {
        SessionRequest request = request(TOMORROW_10, TOMORROW_10.plusDays(1), SessionMode.ONLINE, null);

        assertThatThrownBy(() -> sessionService.createSession(request, mentor(1L, MentorStatus.APPROVED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("истиот ден");
    }

    @Test
    @DisplayName("Сесија во живо мора да има локација")
    void rejectsInPersonWithoutLocation() {
        SessionRequest request = request(TOMORROW_10, TOMORROW_10.plusHours(2), SessionMode.IN_PERSON, "   ");

        assertThatThrownBy(() -> sessionService.createSession(request, mentor(1L, MentorStatus.APPROVED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("локација");
    }

    @Test
    @DisplayName("Најмногу двајца ментори по сесија")
    void rejectsTooManyMentors() {
        User creator = mentor(1L, MentorStatus.APPROVED);
        SessionRequest request = new SessionRequest(
                "Сесија", null, 1L, List.of(2L, 3L),
                SessionMode.ONLINE, null, TOMORROW_10, TOMORROW_10.plusHours(2), null
        );

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(Subject.builder().id(1L).name("Databases").build()));
        when(userRepository.findAllById(List.of(2L, 3L)))
                .thenReturn(List.of(mentor(2L, MentorStatus.APPROVED), mentor(3L, MentorStatus.APPROVED)));

        assertThatThrownBy(() -> sessionService.createSession(request, creator))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining(String.valueOf(BusinessRules.MAX_MENTORS_PER_SESSION));
    }

    @Test
    @DisplayName("Ко-ментор што не е одобрен не може да се додаде на сесија")
    void rejectsUnapprovedCoMentor() {
        SessionRequest request = new SessionRequest(
                "Сесија", null, 1L, List.of(2L),
                SessionMode.ONLINE, null, TOMORROW_10, TOMORROW_10.plusHours(2), null
        );

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(Subject.builder().id(1L).name("Databases").build()));
        when(userRepository.findAllById(List.of(2L)))
                .thenReturn(List.of(mentor(2L, MentorStatus.PENDING)));

        assertThatThrownBy(() -> sessionService.createSession(request, mentor(1L, MentorStatus.APPROVED)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Валидна сесија: креаторот е меѓу менторите, насловот е тримуван, празните тагови се исфрлени")
    void createsValidSession() {
        User creator = mentor(1L, MentorStatus.APPROVED);
        SessionRequest request = request(TOMORROW_10, TOMORROW_10.plusHours(2), SessionMode.ONLINE, null);

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(Subject.builder().id(1L).name("Databases").build()));

        sessionService.createSession(request, creator);

        verify(sessionRepository).save(savedSession.capture());
        Session created = savedSession.getValue();

        assertThat(created.getTitle()).isEqualTo("Подготовка за колоквиум");
        assertThat(created.getMentors()).containsExactly(creator);
        assertThat(created.getTags()).containsExactlyInAnyOrder("колоквиум", "алгоритми");
        assertThat(created.getMaxApplicants()).isEqualTo(BusinessRules.DEFAULT_MAX_APPLICANTS);
        assertThat(created.getMaxApproved()).isEqualTo(BusinessRules.DEFAULT_MAX_APPROVED);
    }

    // -------------------------------------------------- applyToSession: правила

    @Test
    @DisplayName("Не може двапати пријава на иста сесија")
    void rejectsDuplicateApplication() {
        Session session = session(TOMORROW_10, TOMORROW_10.plusHours(2));
        User student = student(5L);

        when(sessionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(session));
        when(applicationRepository.findBySessionAndStudent(session, student))
                .thenReturn(Optional.of(SessionApplication.builder().id(1L).build()));

        assertThatThrownBy(() -> sessionService.applyToSession(7L, student))
                .isInstanceOf(DuplicateApplicationException.class);
    }

    @Test
    @DisplayName("Не може пријава на сесија што веќе се одржала")
    void rejectsApplicationToPastSession() {
        Session past = session(LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusHours(2));

        when(sessionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(past));

        assertThatThrownBy(() -> sessionService.applyToSession(7L, student(5L)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Пријавата се одбива кога сесијата е полна")
    void rejectsApplicationWhenFull() {
        Session session = session(TOMORROW_10, TOMORROW_10.plusHours(2));
        User student = student(5L);

        when(sessionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(session));
        when(applicationRepository.findBySessionAndStudent(session, student)).thenReturn(Optional.empty());
        when(applicationRepository.countBySessionAndStatusNot(session, ApplicationStatus.REJECTED))
                .thenReturn((long) BusinessRules.DEFAULT_MAX_APPLICANTS);

        assertThatThrownBy(() -> sessionService.applyToSession(7L, student))
                .isInstanceOf(SessionFullException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успешна пријава: се зачувува како PENDING и се објавува настан за email")
    void savesPendingApplication() {
        Session session = session(TOMORROW_10, TOMORROW_10.plusHours(2));
        User student = student(5L);

        when(sessionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(session));
        when(applicationRepository.findBySessionAndStudent(session, student)).thenReturn(Optional.empty());
        when(applicationRepository.countBySessionAndStatusNot(session, ApplicationStatus.REJECTED)).thenReturn(3L);
        when(applicationRepository.save(any(SessionApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.applyToSession(7L, student);

        ArgumentCaptor<SessionApplication> saved = ArgumentCaptor.forClass(SessionApplication.class);
        verify(applicationRepository).save(saved.capture());

        assertThat(saved.getValue().getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(saved.getValue().getStudent()).isEqualTo(student);
        verify(events).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Пријава на непостоечка сесија враќа 404-грешка")
    void rejectsApplicationToMissingSession() {
        when(sessionRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.applyToSession(404L, student(5L)))
                .isInstanceOf(mk.focuslab.exception.ResourceNotFoundException.class);
    }

    // ------------------------------------------------------- листа по период

    @Test
    @DisplayName("Календарот бара само својот период, не целата историја")
    void listSessionsUsesRangeQuery() {
        LocalDate from = LocalDate.of(2026, 8, 31);
        LocalDate to = LocalDate.of(2026, 10, 11);

        when(sessionRepository.findStartingBetween(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay())).thenReturn(List.of());

        assertThat(sessionService.listSessions(null, from, to)).isEmpty();

        // без период — стариот пат, целата листа
        verify(sessionRepository, never()).findAllByOrderByStartTimeAsc();
    }

    @Test
    @DisplayName("Половина период е грешка, не тивко игнорирање")
    void listSessionsRejectsHalfRange() {
        assertThatThrownBy(() -> sessionService.listSessions(null, LocalDate.of(2026, 9, 1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("почетен и краен");
    }

    @Test
    @DisplayName("Обратен период е грешка")
    void listSessionsRejectsReversedRange() {
        assertThatThrownBy(() -> sessionService.listSessions(
                null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("пред почетниот");
    }

    // ------------------------------------------------ createSession: работно време

    @Test
    @DisplayName("Пред 08:00 не се закажува")
    void rejectsTooEarly() {
        LocalDateTime start = TOMORROW_10.withHour(7).withMinute(30);
        SessionRequest request = request(start, start.plusHours(1), SessionMode.ONLINE, null);

        assertThatThrownBy(() -> sessionService.createSession(request, mentor(1L, MentorStatus.APPROVED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("08:00");
    }

    @Test
    @DisplayName("По 20:00 не се закажува")
    void rejectsTooLate() {
        LocalDateTime start = TOMORROW_10.withHour(19).withMinute(30);
        SessionRequest request = request(start, start.plusHours(1), SessionMode.ONLINE, null);

        assertThatThrownBy(() -> sessionService.createSession(request, mentor(1L, MentorStatus.APPROVED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20:00");
    }

    @Test
    @DisplayName("Крај точно на 20:00 е дозволен")
    void allowsEndAtClosingTime() {
        LocalDateTime start = TOMORROW_10.withHour(18).withMinute(30);
        SessionRequest request = request(start, start.withHour(20).withMinute(0), SessionMode.ONLINE, null);

        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(Subject.builder().id(1L).name("Databases").build()));

        sessionService.createSession(request, mentor(1L, MentorStatus.APPROVED));

        verify(sessionRepository).save(any());
    }

    @Test
    @DisplayName("Времето мора да е на чекор од 5 минути")
    void rejectsOffStepMinutes() {
        LocalDateTime start = TOMORROW_10.withMinute(37);
        SessionRequest request = request(start, start.plusHours(1), SessionMode.ONLINE, null);

        assertThatThrownBy(() -> sessionService.createSession(request, mentor(1L, MentorStatus.APPROVED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 минути");
    }

    // ------------------------------------------- updateSession / cancelSession

    @Test
    @DisplayName("Само ментор на сесијата може да ја измени")
    void rejectsUpdateByForeignMentor() {
        Session existing = session(TOMORROW_10, TOMORROW_10.plusHours(2));
        existing.setMentors(new java.util.HashSet<>(Set.of(mentor(1L, MentorStatus.APPROVED))));

        when(sessionRepository.findWithSubjectById(7L)).thenReturn(Optional.of(existing));

        SessionRequest request = request(TOMORROW_10, TOMORROW_10.plusHours(2), SessionMode.ONLINE, null);

        assertThatThrownBy(() -> sessionService.updateSession(7L, request, mentor(2L, MentorStatus.APPROVED)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Изменето време → известување до пријавените, со старата и новата вредност")
    void updateNotifiesApplicantsAboutNewTime() {
        User owner = mentor(1L, MentorStatus.APPROVED);
        Session existing = session(TOMORROW_10, TOMORROW_10.plusHours(2));
        existing.setMentors(new java.util.HashSet<>(Set.of(owner)));
        existing.setTitle("Подготовка за колоквиум");

        LocalDateTime newStart = TOMORROW_10.plusHours(4);
        SessionRequest request = request(newStart, newStart.plusHours(1), SessionMode.ONLINE, null);

        when(sessionRepository.findWithSubjectById(7L)).thenReturn(Optional.of(existing));
        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(Subject.builder().id(1L).name("Databases").build()));
        when(applicationRepository.findActiveBySessionId(7L, ApplicationStatus.REJECTED))
                .thenReturn(List.of(application(existing, student(5L), ApplicationStatus.ACCEPTED)));
        when(applicationRepository.countGroupedBySessionAndStatus(List.of(7L))).thenReturn(List.of());

        sessionService.updateSession(7L, request, owner);

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());

        SessionUpdatedEvent event = (SessionUpdatedEvent) published.getValue();
        assertThat(event.recipients()).extracting(Recipient::email).containsExactly("student5@focuslab.mk");
        assertThat(event.changes()).hasSize(1);
        assertThat(event.changes().get(0)).contains("Ново време").contains("претходно");
    }

    @Test
    @DisplayName("Промена само на опис не праќа известување")
    void updateWithoutMeaningfulChangeStaysSilent() {
        User owner = mentor(1L, MentorStatus.APPROVED);
        Session existing = session(TOMORROW_10, TOMORROW_10.plusHours(2));
        existing.setTitle("Подготовка за колоквиум");
        existing.setMentors(new java.util.HashSet<>(Set.of(owner)));

        // Истото време, истиот наслов, истиот предмет — само описот е друг
        SessionRequest request = request(TOMORROW_10, TOMORROW_10.plusHours(2), SessionMode.ONLINE, null);

        when(sessionRepository.findWithSubjectById(7L)).thenReturn(Optional.of(existing));
        when(subjectRepository.findById(1L))
                .thenReturn(Optional.of(Subject.builder().id(1L).name("Databases").build()));
        when(applicationRepository.countGroupedBySessionAndStatus(List.of(7L))).thenReturn(List.of());

        sessionService.updateSession(7L, request, owner);

        verify(events, never()).publishEvent(any());
        verify(applicationRepository, never()).findActiveBySessionId(anyLong(), any());
    }

    @Test
    @DisplayName("Откажување ги брише пријавите и забелешките, и известува пред бришењето")
    void cancelDeletesApplicationsAndNotifies() {
        User owner = mentor(1L, MentorStatus.APPROVED);
        Session existing = session(TOMORROW_10, TOMORROW_10.plusHours(2));
        existing.setMentors(new java.util.HashSet<>(Set.of(owner)));

        when(sessionRepository.findWithSubjectById(7L)).thenReturn(Optional.of(existing));
        when(applicationRepository.findActiveBySessionId(7L, ApplicationStatus.REJECTED))
                .thenReturn(List.of(application(existing, student(5L), ApplicationStatus.PENDING)));

        sessionService.cancelSession(7L, owner);

        verify(applicationRepository).deleteBySessionId(7L);
        verify(noteRepository).deleteBySessionId(7L);
        verify(sessionRepository).delete(existing);

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());

        SessionCancelledEvent event = (SessionCancelledEvent) published.getValue();
        assertThat(event.sessionTitle()).isEqualTo(existing.getTitle());
        assertThat(event.recipients()).hasSize(1);
    }

    @Test
    @DisplayName("Откажување без пријавени не праќа ништо")
    void cancelWithoutApplicantsSendsNothing() {
        User owner = mentor(1L, MentorStatus.APPROVED);
        Session existing = session(TOMORROW_10, TOMORROW_10.plusHours(2));
        existing.setMentors(new java.util.HashSet<>(Set.of(owner)));

        when(sessionRepository.findWithSubjectById(7L)).thenReturn(Optional.of(existing));
        when(applicationRepository.findActiveBySessionId(7L, ApplicationStatus.REJECTED))
                .thenReturn(List.of());

        sessionService.cancelSession(7L, owner);

        verify(sessionRepository).delete(existing);
        verify(events, never()).publishEvent(any());
    }
}
