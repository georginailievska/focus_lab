package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.focuslab.dto.NotificationResponse;
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
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Известувањата во ѕвончето. Ги слуша истите настани како EmailService, но
 * запишува ред во базата наместо да праќа порака.
 *
 * <p>Настаните се слушаат по комит: ако сесијата не се зачува, известување за
 * неа не смее да остане.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    /** Колку известувања се враќаат во списокот на ѕвончето. */
    private static final int LIST_SIZE = 30;

    /** По колку време прочитаните известувања се бришат. */
    private static final Duration KEEP_READ = Duration.ofDays(30);

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final NotificationRepository notificationRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final DtoMapper mapper;

    // ------------------------------------------------------------- читање

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(User user) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(user.getId(), Limit.of(LIST_SIZE))
                .stream()
                .map(mapper::toNotificationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public void markRead(Long id, User user) {
        notificationRepository.markRead(id, user.getId(), Instant.now());
    }

    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllRead(user.getId(), Instant.now());
    }

    // ------------------------------------------------------------- настани

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSessionCreated(SessionCreatedEvent event) {
        Session session = sessionRepository.findWithSubjectById(event.sessionId()).orElse(null);
        if (session == null) {
            return;
        }

        Subject subject = session.getSubject();
        List<User> interested = userRepository.findInterestedInSubject(subject.getId(), Role.STUDENT);

        if (interested.isEmpty()) {
            return;
        }

        List<Notification> batch = new ArrayList<>(interested.size());

        for (User student : interested) {
            batch.add(Notification.builder()
                    .recipient(student)
                    .type(NotificationType.NEW_SESSION)
                    .title("Нова сесија по " + subject.getName())
                    .body(session.getTitle() + " — " + session.getStartTime().format(DATE_TIME))
                    .sessionId(session.getId())
                    .build());
        }

        notificationRepository.saveAll(batch);
        log.debug("Известување за нова сесија {} до {} студенти", session.getId(), batch.size());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationReceived(ApplicationReceivedEvent event) {
        save(
                event.studentId(),
                NotificationType.APPLICATION_RECEIVED,
                "Пријавата е примена",
                event.sessionTitle() + " — чека одлука од менторот",
                event.sessionId()
        );

        // Менторите го добиваат истиот настан од својата страна: има нова пријава
        List<User> mentors = sessionRepository.findMentorsBySessionId(event.sessionId());

        if (mentors.isEmpty()) {
            return;
        }

        List<Notification> batch = new ArrayList<>(mentors.size());

        for (User mentor : mentors) {
            batch.add(build(
                    mentor,
                    NotificationType.NEW_APPLICATION,
                    "Нова пријава",
                    event.studentName() + " се пријави на " + event.sessionTitle(),
                    event.sessionId()
            ));
        }

        notificationRepository.saveAll(batch);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationDecided(ApplicationDecidedEvent event) {
        boolean accepted = event.accepted();

        save(
                event.studentId(),
                accepted ? NotificationType.APPLICATION_ACCEPTED : NotificationType.APPLICATION_REJECTED,
                accepted ? "Пријавата е прифатена" : "Пријавата не е прифатена",
                accepted
                        ? event.sessionTitle() + " — " + event.sessionStartTime().format(DATE_TIME)
                        : event.sessionTitle() + " — побарај друга сесија по истиот предмет",
                event.sessionId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSessionUpdated(SessionUpdatedEvent event) {
        saveForAll(
                event.recipients(),
                NotificationType.SESSION_UPDATED,
                "Променета сесија",
                event.sessionTitle() + " — " + String.join("; ", event.changes()),
                event.sessionId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSessionCancelled(SessionCancelledEvent event) {
        // Без sessionId: сесијата е избришана, нема што да се отвори
        saveForAll(
                event.recipients(),
                NotificationType.SESSION_CANCELLED,
                "Откажана сесија",
                event.sessionTitle() + " — " + event.startTime().format(DATE_TIME) + " е откажана",
                null
        );
    }

    /** Прочитаните известувања не се чуваат бесконечно. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeOld() {
        int removed = notificationRepository.deleteReadOlderThan(Instant.now().minus(KEEP_READ));

        if (removed > 0) {
            log.info("Избришани {} стари прочитани известувања", removed);
        }
    }

    // ------------------------------------------------------------- помошни

    private void saveForAll(
            List<Recipient> recipients,
            NotificationType type,
            String title,
            String body,
            Long sessionId
    ) {
        List<Notification> batch = new ArrayList<>(recipients.size());

        for (Recipient recipient : recipients) {
            User user = userRepository.getReferenceById(recipient.userId());
            batch.add(build(user, type, title, body, sessionId));
        }

        notificationRepository.saveAll(batch);
    }

    private void save(Long recipientId, NotificationType type, String title, String body, Long sessionId) {
        User recipient = userRepository.findById(recipientId).orElse(null);

        if (recipient == null) {
            log.warn("Известувањето се прескокнува — корисникот {} повеќе не постои", recipientId);
            return;
        }

        notificationRepository.save(build(recipient, type, title, body, sessionId));
    }

    private Notification build(
            User recipient,
            NotificationType type,
            String title,
            String body,
            Long sessionId
    ) {
        return Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(cut(title, Notification.MAX_TITLE_LENGTH))
                .body(cut(body, Notification.MAX_BODY_LENGTH))
                .sessionId(sessionId)
                .build();
    }

    /** Насловот на сесијата го пишува ментор — не смее да ја пресече колоната. */
    private static String cut(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }

        return text.substring(0, max - 1) + "…";
    }
}
