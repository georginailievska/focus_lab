package mk.focuslab.repository;

import mk.focuslab.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    // Предметите се прикажуваат во dropdown-и — стабилен азбучен редослед
    List<Subject> findAllByOrderByNameAsc();
}
