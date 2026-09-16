package mk.focuslab.repository;

import mk.focuslab.model.ApplicationStatus;
import mk.focuslab.model.Session;
import mk.focuslab.model.SessionApplication;
import mk.focuslab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Optional;

public interface SessionApplicationRepository extends JpaRepository<SessionApplication, Long> {
    Optional<SessionApplication> findBySessionAndStudent(Session session, User student);

    // За лимитот од 70 вкупно пријавени: сите пријави освен одбиените
    long countBySessionAndStatusNot(Session session, ApplicationStatus status);

    // За лимитот од 24 одобрени
    long countBySessionAndStatus(Session session, ApplicationStatus status);

    List<SessionApplication> findBySessionAndStatus(Session session, ApplicationStatus status);

    // Бројки за профилот на студент — колку пријави има и колку од нив се прифатени
    long countByStudent(User student);

    long countByStudentAndStatus(User student, ApplicationStatus status);

    @Query("""
            select sa.session.id, sa.status, count(sa)
            from SessionApplication sa
            where sa.session.id in :sessionIds
            group by sa.session.id, sa.status
            """)
    List<Object[]> countGroupedBySessionAndStatus(@Param("sessionIds") Collection<Long> sessionIds);

    @Query("""
            select sa from SessionApplication sa
            join fetch sa.student
            join fetch sa.session s
            join fetch s.subject
            where s.id in :sessionIds and sa.status = :status
            order by s.startTime asc
            """)
    List<SessionApplication> findBySessionIdsAndStatus(
            @Param("sessionIds") Collection<Long> sessionIds,
            @Param("status") ApplicationStatus status
    );

    @Query("""
            select sa from SessionApplication sa
            join fetch sa.student
            where sa.session.id = :sessionId and sa.status <> :excluded
            """)
    List<SessionApplication> findActiveBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("excluded") ApplicationStatus excluded
    );

    /** Сите пријави на една сесија — за списокот што го гледа менторот. */
    @Query("""
            select sa from SessionApplication sa
            join fetch sa.student
            join fetch sa.session s
            join fetch s.subject
            where s.id = :sessionId
            order by sa.appliedAt asc
            """)
    List<SessionApplication> findBySessionIdWithStudent(@Param("sessionId") Long sessionId);

    /** Сесиите на кои студентот е прифатен — за видливост на линкот. */
    @Query("""
            select sa.session.id from SessionApplication sa
            where sa.student.id = :studentId and sa.status = :status
            """)
    Set<Long> findSessionIdsByStudentAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") ApplicationStatus status
    );

    @Modifying
    @Query("delete from SessionApplication sa where sa.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);

    /** Пријавите на еден студент ("Upcoming Sessions"), со вчитана сесија. */
    @Query("""
            select sa from SessionApplication sa
            join fetch sa.student
            join fetch sa.session s
            join fetch s.subject
            where sa.student = :student
            order by s.startTime asc
            """)
    List<SessionApplication> findByStudentWithSession(@Param("student") User student);
}
