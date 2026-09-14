package mk.focuslab.repository;

import mk.focuslab.model.SessionNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {

    @EntityGraph(attributePaths = "author")
    List<SessionNote> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    @EntityGraph(attributePaths = "author")
    Optional<SessionNote> findWithAuthorById(Long id);

    @Modifying
    @Query("delete from SessionNote note where note.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
}
