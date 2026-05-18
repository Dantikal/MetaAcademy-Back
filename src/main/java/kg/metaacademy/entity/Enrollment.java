package kg.metaacademy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id","course_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;       // purchasedAt + 30 дней

    @Builder.Default
    private Boolean active = true;

    // Денормализованный прогресс (для быстрого чтения)
    @Builder.Default
    private Integer completedLessons = 0;

    @Builder.Default
    private Integer progressPercent = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Есть ли активный доступ прямо сейчас */
    public boolean hasAccess() {
        return Boolean.TRUE.equals(active) && LocalDateTime.now().isBefore(expiresAt);
    }
}
