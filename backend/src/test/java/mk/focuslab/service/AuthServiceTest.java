package mk.focuslab.service;

import mk.focuslab.dto.RegisterRequest;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.User;
import mk.focuslab.repository.UserRepository;
import mk.focuslab.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String FINKI_DOMAINS = "students.finki.ukim.mk,finki.ukim.mk";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private DtoMapper mapper;

    private ArgumentCaptor<User> savedUser;

    @BeforeEach
    void setUp() {
        savedUser = ArgumentCaptor.forClass(User.class);
    }

    /** Сервисот се составува рачно затоа што листата домени е конструкторски параметар. */
    private AuthService authService(String allowedDomains) {
        return new AuthService(
                userRepository, passwordEncoder, authenticationManager, jwtUtil, mapper, allowedDomains);
    }

    private RegisterRequest request(String email, Role role) {
        return new RegisterRequest("  Ана Николоска  ", email, "password123", role);
    }

    @Test
    @DisplayName("Студентски факултетски email е дозволен")
    void allowsStudentDomain() {
        authService(FINKI_DOMAINS)
                .register(request("ana.nikoloska@students.finki.ukim.mk", Role.STUDENT));

        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo("ana.nikoloska@students.finki.ukim.mk");
        assertThat(savedUser.getValue().getFullName()).isEqualTo("Ана Николоска");
        assertThat(savedUser.getValue().getMentorStatus()).isNull();
    }

    @Test
    @DisplayName("Факултетски email на кадар е дозволен, а ментор добива статус PENDING")
    void allowsStaffDomainAndSetsPendingMentor() {
        authService(FINKI_DOMAINS).register(request("marija@finki.ukim.mk", Role.MENTOR));

        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.MENTOR);
        assertThat(savedUser.getValue().getMentorStatus()).isEqualTo(MentorStatus.PENDING);
    }

    @Test
    @DisplayName("Email од друг домен се одбива")
    void rejectsForeignDomain() {
        assertThatThrownBy(() -> authService(FINKI_DOMAINS)
                .register(request("ana@gmail.com", Role.STUDENT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("факултетски email адреса");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Домен што само личи на факултетски се одбива (точно совпаѓање, не завршница)")
    void rejectsLookalikeDomain() {
        assertThatThrownBy(() -> authService(FINKI_DOMAINS)
                .register(request("ana@zlo-students.finki.ukim.mk", Role.STUDENT)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Големите букви не пречат — email-от се нормализира")
    void normalizesEmailCase() {
        authService(FINKI_DOMAINS)
                .register(request("  ANA@Students.FINKI.UKIM.MK  ", Role.STUDENT));

        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo("ana@students.finki.ukim.mk");
    }

    @Test
    @DisplayName("Празна листа домени значи без ограничување")
    void allowsAnyDomainWhenNotConfigured() {
        authService("").register(request("ana@gmail.com", Role.STUDENT));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("ADMIN не може да се создаде преку регистрација")
    void downgradesAdminRegistrationToStudent() {
        authService(FINKI_DOMAINS).register(request("ana@students.finki.ukim.mk", Role.ADMIN));

        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.STUDENT);
    }
}
