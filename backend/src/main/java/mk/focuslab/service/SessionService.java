package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.ApplicationResponse;
import mk.focuslab.dto.MentorResponse;
import mk.focuslab.dto.SessionRequest;
import mk.focuslab.dto.SessionResponse;
import mk.focuslab.event.ApplicationReceivedEvent;
import mk.focuslab.event.Recipient;
import mk.focuslab.event.SessionCancelledEvent;
import mk.focuslab.event.SessionCreatedEvent;
import mk.focuslab.event.SessionUpdatedEvent;
import mk.focuslab.exception.DuplicateApplicationException;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
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
import mk.focuslab.repository.SubjectRepository;
import mk.focuslab.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {
    private final SessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final SessionApplicationRepository applicationRepository;
    private final ApplicationEventPublisher events;
    private final DtoMapper mapper;

    // ---------------------------------------------------------------- пишување

    @Transactional
    public SessionResponse createSession(SessionRequest request, User creatingMentor) {
        requireApprovedMentor(creatingMentor);
        validateSchedule(request.startTime(), request.endTime());
        validateLocation(request.mode(), request.location());

        Subject subject = findSubjectOrThrow(request.subjectId());

        Session session = Session.builder()
                .title(request.title().trim())
                .description(trimToNull(request.description()))
                .subject(subject)
                .mentors(resolveMentors(request.mentorIds(), creatingMentor))
                .mode(request.mode())
                .location(trimToNull(request.location()))
                .startTime(request.startTime())
                .endTime(request.endTime())
                .tags(normalizeTags(request.tags()))
                .build();

        sessionRepository.save(session);

        // Известувањето се праќа откако транзакцијата ќе се комитира (види EmailService)
        events.publishEvent(new SessionCreatedEvent(session.getId()));

        // Нова сесија — нема ниту една пријава, па не праќаме прашалник за броевите
        return mapper.toSessionResponse(session, 0, 0);
    }

    @Transactional
    public SessionResponse updateSession(Long sessionId, SessionRequest request, User mentor) {
        requireApprovedMentor(mentor);
        validateSchedule(request.startTime(), request.endTime());
        validateLocation(request.mode(), request.location());

        Session session = sessionRepository.findWithSubjectById(sessionId)
                .orElseThrow(() -> sessionNotFound(sessionId));

        requireMentorOfSession(session, mentor);

        Subject subject = findSubjectOrThrow(request.subjectId());
        List<String> changes = describeChanges(session, request, subject);

        session.setTitle(request.title().trim());
        session.setDescription(trimToNull(request.description()));
        session.setSubject(subject);
        session.setMode(request.mode());
        session.setLocation(trimToNull(request.location()));
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());

        session.getMentors().clear();
        session.getMentors().addAll(resolveMentors(request.mentorIds(), mentor));

        session.getTags().clear();
        session.getTags().addAll(normalizeTags(request.tags()));

        sessionRepository.save(session);

        if (!changes.isEmpty()) {
            List<Recipient> recipients = activeRecipients(sessionId);
            if (!recipients.isEmpty()) {
                events.publishEvent(new SessionUpdatedEvent(
                        sessionId, session.getTitle(), changes, recipients));
            }
        }

        Counts counts = countsBySessionId(List.of(sessionId)).getOrDefault(sessionId, Counts.NONE);
        return mapper.toSessionResponse(session, counts.applicants(), counts.approved());
    }

    @Transactional
    public void cancelSession(Long sessionId, User mentor) {
        Session session = sessionRepository.findWithSubjectById(sessionId)
                .orElseThrow(() -> sessionNotFound(sessionId));

        requireMentorOfSession(session, mentor);

        List<Recipient> recipients = activeRecipients(sessionId);
        String title = session.getTitle();
        LocalDateTime startTime = session.getStartTime();

        applicationRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);

        if (!recipients.isEmpty()) {
            events.publishEvent(new SessionCancelledEvent(title, startTime, recipients));
        }
    }

    private List<Recipient> activeRecipients(Long sessionId) {
        return applicationRepository.findActiveBySessionId(sessionId, ApplicationStatus.REJECTED).stream()
                .map(application -> new Recipient(
                        application.getStudent().getEmail(),
                        application.getStudent().getFullName()))
                .toList();
    }

    private void requireMentorOfSession(Session session, User mentor) {
        boolean owns = session.getMentors().stream()
                .anyMatch(existing -> existing.getId().equals(mentor.getId()));

        if (!owns) {
            throw new ForbiddenActionException("Само менторите на сесијата можат да ја менуваат.");
        }
    }

    private List<String> describeChanges(Session session, SessionRequest request, Subject subject) {
        List<String> changes = new ArrayList<>();

        boolean timeChanged = !session.getStartTime().equals(request.startTime())
                || !session.getEndTime().equals(request.endTime());

        if (timeChanged) {
            changes.add("Ново време: " + formatRange(request.startTime(), request.endTime())
                    + " (претходно " + formatRange(session.getStartTime(), session.getEndTime()) + ")");
        }

        if (session.getMode() != request.mode()) {
            changes.add("Начин на одржување: "
                    + (request.mode() == SessionMode.ONLINE ? "online" : "во живо"));
        }

        String newLocation = trimToNull(request.location());
        if (!Objects.equals(session.getLocation(), newLocation)) {
            changes.add(newLocation == null
                    ? "Локацијата е тргната."
                    : "Ново место: " + newLocation);
        }

        String newTitle = request.title().trim();
        if (!session.getTitle().equals(newTitle)) {
            changes.add("Нов наслов: " + newTitle);
        }

        if (!session.getSubject().getId().equals(subject.getId())) {
            changes.add("Нов предмет: " + subject.getName());
        }

        return changes;
    }

    private static final DateTimeFormatter CHANGE_DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private String formatRange(LocalDateTime start, LocalDateTime end) {
        return start.format(CHANGE_DATE_TIME) + " – " + end.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Transactional
    public ApplicationResponse applyToSession(Long sessionId, User student) {
        Session session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> sessionNotFound(sessionId));

        if (session.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ForbiddenActionException("Сесијата веќе се одржа.");
        }

        applicationRepository.findBySessionAndStudent(session, student).ifPresent(existing -> {
            throw new DuplicateApplicationException("Веќе си пријавен/а на оваа сесија.");
        });

        long totalApplied = applicationRepository.countBySessionAndStatusNot(session, ApplicationStatus.REJECTED);
        if (totalApplied >= session.getMaxApplicants()) {
            throw new SessionFullException(
                    "Сесијата ги достигна максималните " + session.getMaxApplicants() + " пријавени.");
        }

        SessionApplication application = applicationRepository.save(
                SessionApplication.builder()
                        .session(session)
                        .student(student)
                        .status(ApplicationStatus.PENDING)
                        .build()
        );

        events.publishEvent(new ApplicationReceivedEvent(
                student.getEmail(), student.getFullName(), session.getTitle()));

        return mapper.toApplicationResponse(application);
    }

    @Transactional
    public void cancelApplication(Long sessionId, User student) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> sessionNotFound(sessionId));

        SessionApplication application = applicationRepository.findBySessionAndStudent(session, student)
                .orElseThrow(() -> new ResourceNotFoundException("Немаш пријава за оваа сесија."));

        applicationRepository.delete(application);
    }

    // ------------------------------------------------------------------ читање

    public List<SessionResponse> listSessions(Long subjectId) {
        return listSessions(subjectId, null, null);
    }

    public List<SessionResponse> listSessions(Long subjectId, LocalDate from, LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Периодот бара и почетен и краен датум.");
        }
        if (from != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Крајниот датум е пред почетниот.");
        }

        List<Session> sessions;

        if (from == null) {
            sessions = (subjectId == null)
                    ? sessionRepository.findAllByOrderByStartTimeAsc()
                    : sessionRepository.findBySubjectOrderByStartTimeAsc(findSubjectOrThrow(subjectId));
        } else {
            LocalDateTime start = from.atStartOfDay();
            LocalDateTime end = to.plusDays(1).atStartOfDay();   // краен ден вклучен

            sessions = (subjectId == null)
                    ? sessionRepository.findStartingBetween(start, end)
                    : sessionRepository.findBySubjectAndStartingBetween(
                            findSubjectOrThrow(subjectId).getId(), start, end);
        }

        return toSessionResponses(sessions);
    }

    public SessionResponse getSession(Long sessionId) {
        Session session = sessionRepository.findWithSubjectById(sessionId)
                .orElseThrow(() -> sessionNotFound(sessionId));

        Counts counts = countsBySessionId(List.of(session.getId()))
                .getOrDefault(session.getId(), Counts.NONE);

        return mapper.toSessionResponse(session, counts.applicants(), counts.approved());
    }

    /** "My Sessions" за ментор (Mentor Dashboard). */
    public List<SessionResponse> listSessionsForMentor(User mentor) {
        return toSessionResponses(sessionRepository.findByMentorId(mentor.getId()));
    }

    /** "Pending Student Requests" за ментор — со еден прашалник за сите негови сесии. */
    public List<ApplicationResponse> listPendingRequestsForMentor(User mentor) {
        List<Long> sessionIds = sessionRepository.findIdsByMentorId(mentor.getId());
        if (sessionIds.isEmpty()) {
            return List.of();
        }

        return mapper.toApplicationResponses(
                applicationRepository.findBySessionIdsAndStatus(sessionIds, ApplicationStatus.PENDING));
    }

    /** "Upcoming Sessions" на студентскиот Home екран. */
    public List<ApplicationResponse> listApplicationsForStudent(User student) {
        return mapper.toApplicationResponses(applicationRepository.findByStudentWithSession(student));
    }

    /** Другите одобрени ментори — за избор на ко-ментор во Create Session. */
    public List<MentorResponse> listApprovedMentorsExcept(User self) {
        return userRepository
                .findByRoleAndMentorStatusAndIdNotOrderByFullNameAsc(Role.MENTOR, MentorStatus.APPROVED, self.getId())
                .stream()
                .map(mapper::toMentorResponse)
                .toList();
    }

    // ---------------------------------------------------------------- помошни

    private List<SessionResponse> toSessionResponses(List<Session> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<Long, Counts> counts = countsBySessionId(sessions.stream().map(Session::getId).toList());

        return sessions.stream()
                .map(session -> {
                    Counts c = counts.getOrDefault(session.getId(), Counts.NONE);
                    return mapper.toSessionResponse(session, c.applicants(), c.approved());
                })
                .toList();
    }

    private Map<Long, Counts> countsBySessionId(List<Long> sessionIds) {
        Map<Long, Counts> result = new HashMap<>();

        for (Object[] row : applicationRepository.countGroupedBySessionAndStatus(sessionIds)) {
            Long sessionId = (Long) row[0];
            ApplicationStatus status = (ApplicationStatus) row[1];
            long count = (Long) row[2];

            Counts current = result.getOrDefault(sessionId, Counts.NONE);
            result.put(sessionId, new Counts(
                    // „пријавени" = сите освен одбиените
                    current.applicants() + (status == ApplicationStatus.REJECTED ? 0 : count),
                    current.approved() + (status == ApplicationStatus.ACCEPTED ? count : 0)
            ));
        }

        return result;
    }

    private void requireApprovedMentor(User mentor) {
        if (mentor.getMentorStatus() != MentorStatus.APPROVED) {
            throw new ForbiddenActionException("Само одобрени ментори можат да закажуваат сесии.");
        }
    }

    /** Сесијата мора да заврши по почетокот и во текот на истиот ден. */
    private void validateSchedule(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Времето на крај мора да биде по времето на почеток.");
        }
        if (!endTime.toLocalDate().equals(startTime.toLocalDate())) {
            throw new IllegalArgumentException("Сесијата мора да започне и заврши во текот на истиот ден.");
        }

        requireWithinWorkingHours(startTime.toLocalTime());
        requireWithinWorkingHours(endTime.toLocalTime());

        requireOnMinuteStep(startTime);
        requireOnMinuteStep(endTime);
    }

    private void requireWithinWorkingHours(LocalTime time) {
        if (time.isBefore(BusinessRules.EARLIEST_START) || time.isAfter(BusinessRules.LATEST_END)) {
            throw new IllegalArgumentException(
                    "Сесиите се закажуваат меѓу " + BusinessRules.EARLIEST_START
                            + " и " + BusinessRules.LATEST_END + " часот.");
        }
    }

    private void requireOnMinuteStep(LocalDateTime moment) {
        boolean onStep = moment.getMinute() % BusinessRules.MINUTE_STEP == 0
                && moment.getSecond() == 0
                && moment.getNano() == 0;

        if (!onStep) {
            throw new IllegalArgumentException(
                    "Времето се задава во чекори од " + BusinessRules.MINUTE_STEP + " минути.");
        }
    }

    /** Ова досега го проверуваше само формата — сега важи и за директни API барања. */
    private void validateLocation(SessionMode mode, String location) {
        if (mode == SessionMode.IN_PERSON && !StringUtils.hasText(location)) {
            throw new IllegalArgumentException("Сесија во живо мора да има локација.");
        }
    }

    private Set<User> resolveMentors(List<Long> requestedMentorIds, User creatingMentor) {
        Set<User> mentors = new HashSet<>();
        mentors.add(creatingMentor);

        List<Long> coMentorIds = requestedMentorIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(creatingMentor.getId()))
                .distinct()
                .toList();

        if (!coMentorIds.isEmpty()) {
            List<User> found = userRepository.findAllById(coMentorIds);
            if (found.size() != coMentorIds.size()) {
                throw new ResourceNotFoundException("Некој од избраните ментори не постои.");
            }

            for (User coMentor : found) {
                if (coMentor.getRole() != Role.MENTOR) {
                    throw new ForbiddenActionException(coMentor.getFullName() + " не е ментор.");
                }
                if (coMentor.getMentorStatus() != MentorStatus.APPROVED) {
                    throw new ForbiddenActionException(coMentor.getFullName() + " сè уште не е одобрен/а како ментор.");
                }
                mentors.add(coMentor);
            }
        }

        if (mentors.size() > BusinessRules.MAX_MENTORS_PER_SESSION) {
            throw new ForbiddenActionException(
                    "Најмногу " + BusinessRules.MAX_MENTORS_PER_SESSION + " ментори по сесија.");
        }

        return mentors;
    }

    /** Празни и празно-место тагови не влегуваат во базата. */
    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) {
            return new HashSet<>();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Subject findSubjectOrThrow(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Не постои предмет со ID " + subjectId));
    }

    private ResourceNotFoundException sessionNotFound(Long sessionId) {
        return new ResourceNotFoundException("Не постои сесија со ID " + sessionId);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Пријавени / одобрени за една сесија. */
    private record Counts(long applicants, long approved) {
        private static final Counts NONE = new Counts(0, 0);
    }
}
