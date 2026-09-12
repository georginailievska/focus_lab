package mk.focuslab.dto;

public record ProfileStats(
        long sessions,
        long upcomingSessions,
        long applications,
        long acceptedApplications
) {
    public static final ProfileStats EMPTY = new ProfileStats(0, 0, 0, 0);
}
