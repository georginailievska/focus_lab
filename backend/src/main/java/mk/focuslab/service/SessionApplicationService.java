package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.ApplicationResponse;
import mk.focuslab.dto.DecisionRequest;
import mk.focuslab.event.ApplicationDecidedEvent;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.exception.SessionFullException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.ApplicationStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.Session;
import mk.focuslab.model.SessionApplication;
import mk.focuslab.model.User;
import mk.focuslab.repository.SessionApplicationRepository;
import mk.focuslab.repository.SessionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SessionApplicationService {
    private final SessionApplicationRepository applicationRepository;
    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher events;
    private final DtoMapper mapper;

    @Transactional
    public ApplicationResponse decide(Long applicationId, DecisionRequest request, User actor) {
        if (request.status() == ApplicationStatus.PENDING) {
            throw new IllegalArgumentException("Одлуката мора да биде ACCEPTED или REJECTED.");
        }

        SessionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Не постои пријава со ID " + applicationId));

        requireCanDecide(application, actor);

        Session session = sessionRepository.findByIdForUpdate(application.getSession().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Сесијата повеќе не постои."));

        boolean accepted = request.status() == ApplicationStatus.ACCEPTED;

        if (accepted) {
            long approvedCount = applicationRepository.countBySessionAndStatus(session, ApplicationStatus.ACCEPTED);
            if (approvedCount >= session.getMaxApproved()) {
                throw new SessionFullException(
                        "Сесијата ги достигна максималните " + session.getMaxApproved() + " одобрени студенти.");
            }
        }

        application.setStatus(request.status());
        application.setDecidedAt(Instant.now());
        applicationRepository.save(application);

        // Настанот носи само готови вредности и се испраќа по комит — види EmailService
        events.publishEvent(new ApplicationDecidedEvent(
                application.getStudent().getId(),
                application.getStudent().getEmail(),
                application.getStudent().getFullName(),
                session.getId(),
                session.getTitle(),
                session.getStartTime(),
                accepted
        ));

        return mapper.toApplicationResponse(application);
    }

    /** Одлучува само ментор на таа сесија, или admin. */
    private void requireCanDecide(SessionApplication application, User actor) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }

        boolean isMentorOfSession = application.getSession().getMentors().stream()
                .anyMatch(mentor -> mentor.getId().equals(actor.getId()));

        if (!isMentorOfSession) {
            throw new ForbiddenActionException("Немаш право да одлучуваш за оваа пријава.");
        }
    }
}
