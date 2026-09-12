package mk.focuslab.event;

public record PasswordChangedEvent(
        String email,
        String fullName
) {
}
