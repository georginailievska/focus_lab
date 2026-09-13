package mk.focuslab.event;

public record PasswordResetRequestedEvent(
        String email,
        String fullName,
        String token,
        long validMinutes
) {
}
