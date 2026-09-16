package mk.focuslab.repository;

import jakarta.persistence.LockModeType;
import mk.focuslab.model.Session;
import mk.focuslab.model.Subject;
import mk.focuslab.model.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    @EntityGraph(attributePaths = "subject")
    List<Session> findAllByOrderByStartTimeAsc();

    @EntityGraph(attributePaths = "subject")
    List<Session> findBySubjectOrderByStartTimeAsc(Subject subject);

    @EntityGraph(attributePaths = "subject")
    Optional<Session> findWithSubjectById(Long id);

    // "My Sessions" за ментор (Mentor Dashboard)
    @EntityGraph(attributePaths = "subject")
    @Query("select s from Session s join s.mentors m where m.id = :mentorId order by s.startTime asc")
    List<Session> findByMentorId(@Param("mentorId") Long mentorId);

    /** Менторите на една сесија — за известувањето при нова пријава. */
    @Query("select m from Session s join s.mentors m where s.id = :sessionId")
    List<User> findMentorsBySessionId(@Param("sessionId") Long sessionId);

    @Query("select s.id from Session s join s.mentors m where m.id = :mentorId")
    List<Long> findIdsByMentorId(@Param("mentorId") Long mentorId);

    @EntityGraph(attributePaths = "subject")
    @Query("select s from Session s where s.startTime >= :from and s.startTime < :to order by s.startTime asc")
    List<Session> findStartingBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @EntityGraph(attributePaths = "subject")
    @Query("""
            select s from Session s
            where s.subject.id = :subjectId and s.startTime >= :from and s.startTime < :to
            order by s.startTime asc
            """)
    List<Session> findBySubjectAndStartingBetween(
            @Param("subjectId") Long subjectId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /** „Нема што да се изземе" — при ново закажување нема постоечка сесија. */
    long NO_EXCLUSION = 0L;

    // Преклопување: почнува пред нашиот крај и завршува по нашиот почеток.
    // Сесија што почнува точно кога другата завршува не е преклопување.
    @EntityGraph(attributePaths = {"subject", "mentors"})
    @Query("""
            select distinct s from Session s
            where s.id <> :excludedId
              and s.startTime < :endTime
              and s.endTime > :startTime
            order by s.startTime asc
            """)
    List<Session> findOverlapping(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedId") long excludedId,
            Limit limit
    );

    // За Admin Overview статистиката ("Active Sessions")
    long countByEndTimeAfter(LocalDateTime time);

    @Query("select count(s) from Session s join s.mentors m where m.id = :mentorId")
    long countByMentorId(@Param("mentorId") Long mentorId);

    @Query("""
            select count(s) from Session s join s.mentors m
            where m.id = :mentorId and s.endTime > :after
            """)
    long countUpcomingByMentorId(@Param("mentorId") Long mentorId, @Param("after") LocalDateTime after);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Session s where s.id = :id")
    Optional<Session> findByIdForUpdate(@Param("id") Long id);
}
