package mk.focuslab.service;

import mk.focuslab.dto.AuthResponse;
import mk.focuslab.dto.LoginRequest;
import mk.focuslab.dto.RegisterRequest;
import mk.focuslab.exception.EmailAlreadyExistsException;
import mk.focuslab.mapper.DtoMapper;
import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.User;
import mk.focuslab.repository.UserRepository;
import mk.focuslab.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final DtoMapper mapper;

    private final Set<String> allowedEmailDomains;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            DtoMapper mapper,
            @Value("${app.auth.allowed-email-domains:}") String allowedEmailDomains
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.mapper = mapper;
        this.allowedEmailDomains = Arrays.stream(allowedEmailDomains.split(","))
                .map(domain -> domain.trim().toLowerCase(Locale.ROOT))
                .filter(domain -> !domain.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        requireAllowedDomain(email);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException("Веќе постои корисник со овој email.");
        }

        // ADMIN се создава само преку AdminSeeder, никогаш преку оваа рута
        Role role = request.role() == Role.ADMIN ? Role.STUDENT : request.role();

        User user = userRepository.save(
                User.builder()
                        .fullName(request.fullName().trim())
                        .email(email)
                        .passwordHash(passwordEncoder.encode(request.password()))
                        .role(role)
                        .mentorStatus(role == Role.MENTOR ? MentorStatus.PENDING : null)
                        .build()
        );

        return mapper.toAuthResponse(user, jwtUtil.generateToken(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        // Ако лозинката е погрешна, тука лета BadCredentialsException → 401
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Корисникот исчезна веднаш по автентикација."));

        return mapper.toAuthResponse(user, jwtUtil.generateToken(user));
    }

    private void requireAllowedDomain(String email) {
        if (allowedEmailDomains.isEmpty()) {
            return;
        }

        int at = email.lastIndexOf('@');
        String domain = at < 0 ? "" : email.substring(at + 1);

        if (!allowedEmailDomains.contains(domain)) {
            throw new IllegalArgumentException(
                    "Регистрацијата е дозволена само со факултетски email адреса.");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
