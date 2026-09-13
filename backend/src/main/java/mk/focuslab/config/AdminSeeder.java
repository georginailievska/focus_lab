package mk.focuslab.config;

import lombok.extern.slf4j.Slf4j;
import mk.focuslab.model.Role;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import mk.focuslab.repository.SubjectRepository;
import mk.focuslab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
public class AdminSeeder implements ApplicationRunner {
    private static final List<String> DEFAULT_SUBJECTS = List.of(
            "Математика 1",
            "Структурно програмирање",
            "Примена на алгоритми и податочни структури",
            "Математика 3",
            "Бази на податоци"
    );

    private static final Map<String, String> RENAMED_SUBJECTS = Map.of(
            "Mathematics 1", "Математика 1",
            "Structured Programming", "Структурно програмирање",
            "Algorithms and Data Structures", "Примена на алгоритми и податочни структури",
            "Mathematics 3", "Математика 3",
            "Databases", "Бази на податоци"
    );

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        renameSubjects();
        seedSubjects();
    }

    private void seedAdmin() {
        String email = adminEmail.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        userRepository.save(User.builder()
                .fullName("Admin")
                .email(email)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build());

        log.info("Seeded admin account: {}", email);
    }

    private void renameSubjects() {
        RENAMED_SUBJECTS.forEach((oldName, newName) -> subjectRepository
                .findByNameIgnoreCase(oldName)
                .filter(subject -> !subjectRepository.existsByNameIgnoreCase(newName))
                .ifPresent(subject -> {
                    subject.setName(newName);
                    subjectRepository.save(subject);
                    log.info("Renamed subject '{}' -> '{}'", oldName, newName);
                }));
    }

    private void seedSubjects() {
        DEFAULT_SUBJECTS.stream()
                .filter(name -> !subjectRepository.existsByNameIgnoreCase(name))
                .forEach(name -> subjectRepository.save(Subject.builder().name(name).build()));
    }
}
