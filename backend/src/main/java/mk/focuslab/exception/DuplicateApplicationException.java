package mk.focuslab.exception;

// Се фрла кога студент се обидува да се пријави двапати на истата сесија
public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException(String message) {
        super(message);
    }
}
