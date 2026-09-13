package mk.focuslab.exception;

// Пр. ментор со MentorStatus != APPROVED се обидува да закаже сесија
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
