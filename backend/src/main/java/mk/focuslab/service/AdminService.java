package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.AdminStatsResponse;
import mk.focuslab.dto.UserResponse;
import mk.focuslab.exception.ForbiddenActionException;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.User;
import mk.focuslab.repository.SessionRepository;
import mk.focuslab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final DtoMapper mapper;

    /** Controller-от го повикува со MentorStatus.APPROVED или .REJECTED. */
    @Transactional
    public UserResponse decideMentor(Long userId, MentorStatus decision) {
        User mentor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Не постои корисник со ID " + userId));

        if (mentor.getRole() != Role.MENTOR) {
            throw new ForbiddenActionException(mentor.getFullName() + " не е регистриран/а како ментор.");
        }

        mentor.setMentorStatus(decision);
        userRepository.save(mentor);

        return mapper.toUserResponse(mentor);
    }

    public List<UserResponse> listPendingMentors() {
        return mapper.toUserResponses(
                userRepository.findByRoleAndMentorStatusOrderByCreatedAtAsc(Role.MENTOR, MentorStatus.PENDING));
    }

    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.MENTOR),
                sessionRepository.countByEndTimeAfter(LocalDateTime.now())
        );
    }
}
