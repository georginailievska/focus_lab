package mk.focuslab.repository;

import mk.focuslab.model.PostComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    @EntityGraph(attributePaths = {"author", "post"})
    Optional<PostComment> findWithAuthorById(Long id);
}
