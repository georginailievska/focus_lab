package mk.focuslab.service;

import mk.focuslab.dto.InterestsRequest;
import mk.focuslab.dto.SubjectResponse;
import mk.focuslab.exception.ResourceNotFoundException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.Role;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.SubjectRepository;
import mk.focuslab.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private InterestService interestService;

    private Subject subject(long id, String name) {
        return Subject.builder().id(id).name(name).build();
    }

    private User studentWithInterests(Subject... interests) {
        Set<Subject> current = new HashSet<>(List.of(interests));

        return User.builder()
                .id(5L)
                .fullName("Ана Николоска")
                .email("ana@students.finki.ukim.mk")
                .passwordHash("x")
                .role(Role.STUDENT)
                .interests(current)
                .build();
    }

    /** Мапирањето е вистинско само каде резултатот се проверува. */
    private void stubMapper() {
        when(mapper.toSubjectResponse(any(Subject.class))).thenAnswer(invocation -> {
            Subject subject = invocation.getArgument(0);
            return new SubjectResponse(subject.getId(), subject.getName());
        });
    }

    @Test
    @DisplayName("Новата листа ја заменува старата целосно")
    void replacesWholeSelection() {
        Subject databases = subject(2L, "Databases");
        Subject algorithms = subject(3L, "Algorithms and Data Structures");
        User student = studentWithInterests(subject(1L, "Mathematics 1"));

        when(userRepository.findWithInterestsById(5L)).thenReturn(Optional.of(student));
        when(subjectRepository.findAllById(anyIterable())).thenReturn(List.of(databases, algorithms));
        stubMapper();

        List<SubjectResponse> result =
                interestService.replaceInterests(student, new InterestsRequest(List.of(2L, 3L)));

        // Стариот интерес (Mathematics 1) го нема повеќе
        assertThat(student.getInterests()).containsExactlyInAnyOrder(databases, algorithms);
        verify(userRepository).save(student);

        // Резултатот е азбучно сортиран
        assertThat(result).extracting(SubjectResponse::name)
                .containsExactly("Algorithms and Data Structures", "Databases");
    }

    @Test
    @DisplayName("Празна листа ги брише сите интереси")
    void emptySelectionClearsInterests() {
        User student = studentWithInterests(subject(1L, "Mathematics 1"), subject(2L, "Databases"));

        when(userRepository.findWithInterestsById(5L)).thenReturn(Optional.of(student));

        List<SubjectResponse> result =
                interestService.replaceInterests(student, new InterestsRequest(List.of()));

        assertThat(student.getInterests()).isEmpty();
        assertThat(result).isEmpty();
        verify(subjectRepository, never()).findAllById(anyIterable());
    }

    @Test
    @DisplayName("Непостоечки предмет се одбива и не менува ништо")
    void rejectsUnknownSubject() {
        Subject databases = subject(2L, "Databases");
        User student = studentWithInterests(databases);

        when(userRepository.findWithInterestsById(5L)).thenReturn(Optional.of(student));
        // Побарани два, најден еден → некој ID не постои
        when(subjectRepository.findAllById(anyIterable())).thenReturn(List.of(databases));

        assertThatThrownBy(() ->
                interestService.replaceInterests(student, new InterestsRequest(List.of(2L, 999L))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Дупликат ID-а во барањето не се проблем")
    void ignoresDuplicateIds() {
        Subject databases = subject(2L, "Databases");
        User student = studentWithInterests();

        when(userRepository.findWithInterestsById(5L)).thenReturn(Optional.of(student));
        when(subjectRepository.findAllById(anyIterable())).thenReturn(List.of(databases));
        stubMapper();

        List<SubjectResponse> result =
                interestService.replaceInterests(student, new InterestsRequest(List.of(2L, 2L)));

        assertThat(student.getInterests()).containsExactly(databases);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Читањето на интересите е азбучно сортирано")
    void listsInterestsSorted() {
        User student = studentWithInterests(
                subject(3L, "Mathematics 1"), subject(1L, "Databases"), subject(2L, "Algorithms"));

        when(userRepository.findWithInterestsById(5L)).thenReturn(Optional.of(student));
        stubMapper();

        assertThat(interestService.listInterests(student)).extracting(SubjectResponse::name)
                .containsExactly("Algorithms", "Databases", "Mathematics 1");
    }
}
