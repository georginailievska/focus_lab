package mk.focuslab.event;

/** Кому се праќа известување — id-то е за известувањата во апликацијата. */
public record Recipient(Long userId, String email, String fullName) {
}
