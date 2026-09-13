package mk.focuslab.model;

import java.time.LocalTime;

public final class BusinessRules {
    /** Една сесија може да ја водат најмногу двајца ментори. */
    public static final int MAX_MENTORS_PER_SESSION = 2;

    /** Вкупно пријавени (сите статуси освен REJECTED) по сесија. */
    public static final int DEFAULT_MAX_APPLICANTS = 70;

    /** Одобрени (ACCEPTED) студенти по сесија. */
    public static final int DEFAULT_MAX_APPROVED = 24;

    public static final LocalTime EARLIEST_START = LocalTime.of(8, 0);
    public static final LocalTime LATEST_END = LocalTime.of(20, 0);

    public static final int MINUTE_STEP = 5;

    private BusinessRules() {
        // помошна класа со константи — не се инстанцира
    }
}
