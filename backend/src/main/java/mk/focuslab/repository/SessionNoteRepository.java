package mk.focuslab.repository;

import mk.focuslab.model.SessionNote;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {

    /** „Сите предмети" — сервисот го праќа ова наместо {@code null}. */
    long ANY_SUBJECT = 0L;

    @EntityGraph(attributePaths = "author")
    List<SessionNote> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    // Предметот е нула наместо null: Postgres не може да го погоди типот на
    // параметар што се споредува само со null во JPQL.
    @EntityGraph(attributePaths = {"author", "session", "session.subject"})
    @Query("""
            select note from SessionNote note
            where :subjectId = 0 or note.session.subject.id = :subjectId
            order by note.createdAt desc
            """)
    List<SessionNote> findForOverview(@Param("subjectId") long subjectId, Limit limit);

    @EntityGraph(attributePaths = "author")
    Optional<SessionNote> findWithAuthorById(Long id);

    @Modifying
    @Query("delete from SessionNote note where note.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
}
