package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.StudentCommentRequest;
import mk.focuslab.dto.StudentCommentResponse;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.StudentComment;
import mk.focuslab.model.User;
import mk.focuslab.repository.StudentCommentRepository;
import mk.focuslab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Коментари на ментори за студент; авторот избира дали се делат со другите ментори. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCommentService {
    private final StudentCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final DtoMapper mapper;

    public List<StudentCommentResponse> listComments(Long studentId, User viewer) {
        requireApprovedMentor(viewer);
        requireStudent(studentId);

        return commentRepository.findVisibleForStudent(studentId, viewer.getId()).stream()
                .map(mapper::toStudentCommentResponse)
                .toList();
    }

    @Transactional
    public StudentCommentResponse addComment(Long studentId, StudentCommentRequest request, User author) {
        requireApprovedMentor(author);
        User student = requireStudent(studentId);

        StudentComment comment = commentRepository.save(StudentComment.builder()
                .student(student)
                .author(author)
                .text(request.text().trim())
                .sharedWithMentors(request.sharedWithMentors())
                .build());

        return mapper.toStudentCommentResponse(comment);
    }

    /** Видливоста се менува и подоцна — авторот се премислува. */
    @Transactional
    public StudentCommentResponse changeVisibility(Long commentId, boolean shared, User actor) {
        StudentComment comment = requireOwnComment(commentId, actor);

        comment.setSharedWithMentors(shared);
        commentRepository.save(comment);

        return mapper.toStudentCommentResponse(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, User actor) {
        commentRepository.delete(requireOwnComment(commentId, actor));
    }

    private StudentComment requireOwnComment(Long commentId, User actor) {
        requireApprovedMentor(actor);

        StudentComment comment = commentRepository.findWithAuthorById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Коментарот не постои."));

        if (!comment.getAuthor().getId().equals(actor.getId())) {
            throw new ForbiddenActionException("Секој ментор менува само свои коментари.");
        }

        return comment;
    }

    // Улогата MENTOR не значи и APPROVED, па одобрувањето се проверува тука
    private void requireApprovedMentor(User user) {
        if (user.getRole() != Role.MENTOR || user.getMentorStatus() != MentorStatus.APPROVED) {
            throw new ForbiddenActionException("Коментарите се достапни само за одобрени ментори.");
        }
    }

    // Коментар има смисла само за студент; ментор или админ не е цел
    private User requireStudent(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Корисникот не постои."));

        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenActionException("Коментари се оставаат само на профил на студент.");
        }

        return student;
    }
}
