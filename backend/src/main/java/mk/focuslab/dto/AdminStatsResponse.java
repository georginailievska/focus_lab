package mk.focuslab.dto;

public record AdminStatsResponse(
        long totalUsers,
        long students,
        long mentors,
        long activeSessions
) {
}
