package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.focuslab.event.ApplicationDecidedEvent;
import mk.focuslab.event.ApplicationReceivedEvent;
import mk.focuslab.event.PasswordChangedEvent;
import mk.focuslab.event.PasswordResetRequestedEvent;
import mk.focuslab.event.Recipient;
import mk.focuslab.event.SessionCancelledEvent;
import mk.focuslab.event.SessionCreatedEvent;
import mk.focuslab.event.SessionUpdatedEvent;
import mk.focuslab.model.ApplicationStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.Session;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.SessionApplicationRepository;
import mk.focuslab.repository.SessionRepository;
import mk.focuslab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String SIGNATURE = "\n\n— FOCUS Lab";

    private final JavaMailSender mailSender;
    private final SessionRepository sessionRepository;
    private final SessionApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Value("${app.mail.from}")
    private String from;

    /** За линкот до сесијата во известувањата. */
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationReceived(ApplicationReceivedEvent event) {
        send(
                event.studentEmail(),
                "FOCUS Lab — пријавата е примена",
                "Здраво " + event.studentName() + ",\n\n"
                        + "Твојата пријава за сесијата \"" + event.sessionTitle() + "\" е примена и чека одлука."
                        + SIGNATURE
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationDecided(ApplicationDecidedEvent event) {
        String body = event.accepted()
                ? "Прифатена си на сесијата \"" + event.sessionTitle() + "\", "
                        + event.sessionStartTime().format(DATE_TIME) + ".\n\n"
                        + "Детали: " + link("/sessions/" + event.sessionId())
                : "За жал, овој пат не си прифатена на сесијата \"" + event.sessionTitle() + "\".\n\n"
                        + "Местата се ограничени — побарај друга сесија по истиот предмет: "
                        + link("/sessions");

        send(
                event.studentEmail(),
                "FOCUS Lab — " + (event.accepted() ? "пријавата е прифатена" : "пријавата е одбиена"),
                "Здраво " + event.studentName() + ",\n\n" + body + SIGNATURE
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionUpdated(SessionUpdatedEvent event) {
        String changes = event.changes().stream()
                .map(change -> "• " + change)
                .collect(Collectors.joining("\n"));

        for (Recipient recipient : event.recipients()) {
            send(
                    recipient.email(),
                    "FOCUS Lab — променета сесија: " + event.sessionTitle(),
                    "Здраво " + recipient.fullName() + ",\n\n"
                            + "Сесијата \"" + event.sessionTitle() + "\" е изменета:\n\n"
                            + changes + "\n\n"
                            + "Детали: " + link("/sessions/" + event.sessionId())
                            + SIGNATURE
            );
        }

        log.info("Известување за променета сесија {} до {} студенти",
                event.sessionId(), event.recipients().size());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCancelled(SessionCancelledEvent event) {
        for (Recipient recipient : event.recipients()) {
            send(
                    recipient.email(),
                    "FOCUS Lab — откажана сесија: " + event.sessionTitle(),
                    "Здраво " + recipient.fullName() + ",\n\n"
                            + "Сесијата \"" + event.sessionTitle() + "\", закажана за "
                            + event.startTime().format(DATE_TIME) + ", е откажана.\n\n"
                            + "Прегледај други сесии: " + link("/sessions")
                            + SIGNATURE
            );
        }

        log.info("Известување за откажана сесија до {} студенти", event.recipients().size());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        send(
                event.email(),
                "FOCUS Lab — нова лозинка",
                "Здраво " + event.fullName() + ",\n\n"
                        + "Побарана е нова лозинка за твојот профил. Отвори го линкот за да ја поставиш:\n\n"
                        + frontendBaseUrl.trim() + "/reset-password?token=" + event.token() + "\n\n"
                        + "Линкот важи " + event.validMinutes()
                        + " минути и може да се искористи само еднаш.\n"
                        + "Ако не си го побарала ти, игнорирај ја пораката — лозинката останува непроменета."
                        + SIGNATURE
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        send(
                event.email(),
                "FOCUS Lab — лозинката е променета",
                "Здраво " + event.fullName() + ",\n\n"
                        + "Лозинката за твојот профил е успешно променета.\n\n"
                        + "Ако ти не ја смени, побарај нова лозинка веднаш преку "
                        + "\"Заборавена лозинка\" на страницата за најава."
                        + SIGNATURE
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onSessionCreated(SessionCreatedEvent event) {
        Session session = sessionRepository.findWithSubjectById(event.sessionId()).orElse(null);
        if (session == null) {
            log.warn("Сесијата {} повеќе не постои — известувањето се прескокнува", event.sessionId());
            return;
        }

        Subject subject = session.getSubject();
        List<User> interested = userRepository.findInterestedInSubject(subject.getId(), Role.STUDENT);

        if (interested.isEmpty()) {
            log.debug("Нема студенти со интерес за {} — нема кому да се прати", subject.getName());
            return;
        }

        log.info("Известување за нова сесија по {} до {} заинтересирани студенти",
                subject.getName(), interested.size());

        for (User student : interested) {
            send(
                    student.getEmail(),
                    "FOCUS Lab — нова сесија по " + subject.getName(),
                    "Здраво " + student.getFullName() + ",\n\n"
                            + "Закажана е нова сесија по " + subject.getName() + ", предмет што го избра "
                            + "како свој интерес:\n\n"
                            + "\"" + session.getTitle() + "\"\n"
                            + session.getStartTime().format(DATE_TIME) + "\n\n"
                            + "Отвори и пријави се: " + sessionLink(session)
                            + SIGNATURE
            );
        }
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendSessionReminders() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        // Порано ова ги вчитуваше СИТЕ сесии и филтрираше во меморија
        for (Session session : sessionRepository.findStartingBetween(startOfDay, startOfNextDay)) {
            applicationRepository.findBySessionAndStatus(session, ApplicationStatus.ACCEPTED)
                    .forEach(application -> send(
                            application.getStudent().getEmail(),
                            "FOCUS Lab — потсетник за денешната сесија",
                            "Здраво " + application.getStudent().getFullName() + ",\n\n"
                                    + "Потсетник: сесијата \"" + session.getTitle() + "\" е денес во "
                                    + session.getStartTime().format(DATE_TIME) + ".\n\n"
                                    + "Детали: " + sessionLink(session)
                                    + SIGNATURE
                    ));
        }
    }

    private String sessionLink(Session session) {
        return link("/sessions/" + session.getId());
    }

    private String link(String path) {
        return frontendBaseUrl.trim() + path;
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.debug("Испратен email до {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("Не успеа испраќање email до {}: {}", to, e.getMessage());
        }
    }
}
