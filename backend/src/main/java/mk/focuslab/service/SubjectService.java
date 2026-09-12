package mk.focuslab.service;

import lombok.RequiredArgsConstructor;
import mk.focuslab.dto.SubjectRequest;
import mk.focuslab.dto.SubjectResponse;
import mk.focuslab.exception.DuplicateResourceException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.Subject;
import mk.focuslab.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final DtoMapper mapper;

    public List<SubjectResponse> listSubjects() {
        return subjectRepository.findAllByOrderByNameAsc().stream()
                .map(mapper::toSubjectResponse)
                .toList();
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        String name = request.name().trim();

        if (subjectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Веќе постои предмет со име \"" + name + "\".");
        }

        return mapper.toSubjectResponse(subjectRepository.save(Subject.builder().name(name).build()));
    }
}
