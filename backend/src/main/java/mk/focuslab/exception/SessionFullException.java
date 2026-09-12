package mk.focuslab.exception;

// Се фрла кога вкупно пријавени >= 70 или одобрени >= 24 (деловни правила)
public class SessionFullException extends RuntimeException {
    public SessionFullException(String message) {
        super(message);
    }
}
