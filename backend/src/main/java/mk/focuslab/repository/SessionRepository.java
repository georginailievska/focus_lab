package mk.focuslab.repository;

import jakarta.persistence.LockModeType;
import mk.focuslab.model.Session;
import mk.focuslab.model.Subject;
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
