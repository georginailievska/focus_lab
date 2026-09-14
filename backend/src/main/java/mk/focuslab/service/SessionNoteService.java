package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.SessionNoteRequest;
import mk.focuslab.dto.SessionNoteResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Забелешки за сесија, видливи за секој одобрен ментор и никогаш за студент. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionNoteService {
    private final SessionNoteRepository noteRepository;
    private final SessionRepository sessionRepository;
    private final DtoMapper mapper;

    public List<SessionNoteResponse> listNotes(Long sessionId, User viewer) {
        requireApprovedMentor(viewer);
        requireSessionExists(sessionId);

        return noteRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(mapper::toSessionNoteResponse)
                .toList();
    }

    @Transactional
    public SessionNoteResponse addNote(Long sessionId, SessionNoteRequest request, User author) {
        requireApprovedMentor(author);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> sessionNotFound(sessionId));

        SessionNote note = noteRepository.save(SessionNote.builder()
                .session(session)
                .author(author)
                .text(request.text().trim())
                .build());

        return mapper.toSessionNoteResponse(note);
    }

    @Transactional
    public void deleteNote(Long noteId, User actor) {
        requireApprovedMentor(actor);

        SessionNote note = noteRepository.findWithAuthorById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Забелешката не постои."));

        if (!note.getAuthor().getId().equals(actor.getId())) {
            throw new ForbiddenActionException("Секој ментор брише само свои забелешки.");
        }

        noteRepository.delete(note);
    }

    // Улогата MENTOR не значи и APPROVED, па одобрувањето се проверува тука
    private void requireApprovedMentor(User user) {
        if (user.getRole() != Role.MENTOR || user.getMentorStatus() != MentorStatus.APPROVED) {
            throw new ForbiddenActionException("Забелешките се достапни само за одобрени ментори.");
        }
    }

    private void requireSessionExists(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw sessionNotFound(sessionId);
        }
    }

    private ResourceNotFoundException sessionNotFound(Long sessionId) {
        return new ResourceNotFoundException("Сесијата со id " + sessionId + " не постои.");
    }
}
