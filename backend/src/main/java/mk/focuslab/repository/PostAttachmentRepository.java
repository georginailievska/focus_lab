package mk.focuslab.repository;

import mk.focuslab.model.PostAttachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {
    /** Прилогот со своите бајти — само кога навистина се служи датотеката. */
    @EntityGraph(attributePaths = "blob")
    Optional<PostAttachment> findWithBlobByAccessKey(String accessKey);
}
