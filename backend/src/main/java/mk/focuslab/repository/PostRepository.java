package mk.focuslab.repository;

import mk.focuslab.model.Post;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    /** „Нема курсор" — почни од најновата објава. */
    long NEWEST_FIRST = Long.MAX_VALUE;

    /** „Сите предмети" — сервисот го праќа ова наместо {@code null}. */
    long ANY_SUBJECT = 0L;

    @EntityGraph(attributePaths = {"author", "subject"})
    @Query("""
            select p from Post p
            where p.id < :beforeId
              and (:subjectId = 0 or p.subject.id = :subjectId)
            order by p.id desc
            """)
    List<Post> findFeed(
            @Param("beforeId") long beforeId,
            @Param("subjectId") long subjectId,
            Limit limit
    );

    @EntityGraph(attributePaths = {"author", "subject"})
    Optional<Post> findWithAuthorById(Long id);
}
