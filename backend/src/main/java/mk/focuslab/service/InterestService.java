package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.InterestsRequest;
import mk.focuslab.dto.SubjectResponse;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.SubjectRepository;
import mk.focuslab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final DtoMapper mapper;

    public List<SubjectResponse> listInterests(User student) {
        return toSortedResponse(loadWithInterests(student.getId()).getInterests());
    }

    @Transactional
    public List<SubjectResponse> replaceInterests(User student, InterestsRequest request) {
        User managed = loadWithInterests(student.getId());

        managed.getInterests().clear();
        managed.getInterests().addAll(resolveSubjects(request.subjectIds()));
        userRepository.save(managed);

        return toSortedResponse(managed.getInterests());
    }

    private Set<Subject> resolveSubjects(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(subjectIds);
        List<Subject> found = subjectRepository.findAllById(uniqueIds);

        if (found.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("Некој од избраните предмети не постои.");
        }

        return new HashSet<>(found);
    }

    private User loadWithInterests(Long userId) {
        return userRepository.findWithInterestsById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Не постои корисник со ID " + userId));
    }

    private List<SubjectResponse> toSortedResponse(Set<Subject> subjects) {
        return subjects.stream()
                .map(mapper::toSubjectResponse)
                .sorted(Comparator.comparing(SubjectResponse::name))
                .toList();
    }
}
