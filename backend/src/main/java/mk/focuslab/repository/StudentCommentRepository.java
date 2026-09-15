package mk.focuslab.repository;

import mk.focuslab.model.StudentComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentCommentRepository extends JpaRepository<StudentComment, Long> {

    // Филтрирањето е во базата, не во Java: така приватен коментар воопшто не
    // излегува од базата за ментор што не смее да го види.
    @EntityGraph(attributePaths = "author")
    @Query("""
            select comment from StudentComment comment
            where comment.student.id = :studentId
              and (comment.sharedWithMentors = true or comment.author.id = :viewerId)
            order by comment.createdAt desc
            """)
    List<StudentComment> findVisibleForStudent(
            @Param("studentId") Long studentId,
            @Param("viewerId") Long viewerId
    );

    @EntityGraph(attributePaths = "author")
    Optional<StudentComment> findWithAuthorById(Long id);
}
