package mk.focuslab.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToMany
    @JoinTable(
            name = "session_mentors",
            joinColumns = @JoinColumn(name = "session_id"),
            inverseJoinColumns = @JoinColumn(name = "mentor_id")
    )
    @BatchSize(size = 50)
    @Builder.Default
    private Set<User> mentors = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionMode mode;

    // Локација (IN_PERSON) или линк за средба (ONLINE, пр. Teams линк)
    private String location;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    /** Вкупно пријавени (сите статуси освен REJECTED) — види BusinessRules. */
    @Column(nullable = false)
    @Builder.Default
    private int maxApplicants = BusinessRules.DEFAULT_MAX_APPLICANTS;

    /** Одобрени (ACCEPTED) — види BusinessRules. */
    @Column(nullable = false)
    @Builder.Default
    private int maxApproved = BusinessRules.DEFAULT_MAX_APPROVED;

    @ElementCollection
    @CollectionTable(name = "session_tags", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "tag")
    @BatchSize(size = 50)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
