package mk.focuslab.repository;

import mk.focuslab.model.MentorStatus;
import mk.focuslab.model.Role;
import mk.focuslab.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // За admin екранот: листа ментори кои чекаат одобрување
    List<User> findByRoleAndMentorStatusOrderByCreatedAtAsc(Role role, MentorStatus mentorStatus);

    List<User> findByRoleAndMentorStatusAndIdNotOrderByFullNameAsc(
            Role role, MentorStatus mentorStatus, Long excludedId
    );

    long countByRole(Role role);

    /** Корисникот заедно со неговите интереси — за екранот „Мои интереси". */
    @EntityGraph(attributePaths = "interests")
    Optional<User> findWithInterestsById(Long id);

    @Query("""
            select u from User u
            join u.interests interest
            where interest.id = :subjectId and u.role = :role
            order by u.fullName asc
            """)
    List<User> findInterestedInSubject(@Param("subjectId") Long subjectId, @Param("role") Role role);
}
