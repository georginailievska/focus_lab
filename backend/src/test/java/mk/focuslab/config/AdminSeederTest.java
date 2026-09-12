package mk.focuslab.config;

import mk.focuslab.model.Subject;
import mk.focuslab.repository.SubjectRepository;
import mk.focuslab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    /** Мал „ин-мемори" репозиториум: клуч е името со мали букви, како unique во базата. */
    private final Map<String, Subject> rows = new LinkedHashMap<>();

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    @BeforeEach
    void stubRepositories() {
        // Админот веќе постои — тука не е предмет на тестирање
        lenient().when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        lenient().when(subjectRepository.existsByNameIgnoreCase(anyString()))
                .thenAnswer(call -> rows.containsKey(key(call.getArgument(0))));

        lenient().when(subjectRepository.findByNameIgnoreCase(anyString()))
                .thenAnswer(call -> Optional.ofNullable(rows.get(key(call.getArgument(0)))));

        lenient().when(subjectRepository.save(any(Subject.class))).thenAnswer(call -> {
            Subject saved = call.getArgument(0);
            // При преименување истиот објект стои под стариот клуч — тој си оди
            rows.values().removeIf(existing -> existing == saved);
            rows.put(key(saved.getName()), saved);
            return saved;
        });
    }

    private AdminSeeder seeder() {
        return new AdminSeeder(userRepository, subjectRepository, passwordEncoder,
                "admin@focuslab.mk", "tajna");
    }

    @Test
    @DisplayName("Англиското име се преименува во истиот ред, без нов предмет")
    void renamesSubjectKeepingTheSameRow() {
        Subject databases = Subject.builder().id(7L).name("Databases").build();
        rows.put(key("Databases"), databases);

        seeder().run(null);

        assertThat(databases.getName()).isEqualTo("Бази на податоци");
        assertThat(rows.get(key("Бази на податоци"))).isSameAs(databases);
        assertThat(rows).hasSize(5);
    }

    @Test
    @DisplayName("Празна база: внесува пет предмети со македонски имиња")
    void seedsMacedonianNamesIntoEmptyDatabase() {
        seeder().run(null);

        assertThat(rows.keySet()).containsExactlyInAnyOrder(
                key("Математика 1"),
                key("Структурно програмирање"),
                key("Примена на алгоритми и податочни структури"),
                key("Математика 3"),
                key("Бази на податоци"));
    }

    @Test
    @DisplayName("Вториот старт не менува ништо")
    void secondRunWritesNothing() {
        seeder().run(null);
        clearInvocations(subjectRepository);

        seeder().run(null);

        verify(subjectRepository, never()).save(any(Subject.class));
        assertThat(rows).hasSize(5);
    }
}
