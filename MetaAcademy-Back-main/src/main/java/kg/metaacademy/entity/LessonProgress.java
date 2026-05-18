package kg.metaacademy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id","lesson_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Builder.Default
    private Boolean videoWatched = false;

    @Builder.Default
    private Integer quizAttempts = 0;     // кол-во попыток (10/5/3 баллов)

    @Builder.Default
    private Boolean quizPassed = false;

    @Builder.Default
    private Boolean practiceDone = false;

    // Поля для отслеживания попыток файловой практики
    private Integer practiceAttemptsToday;
    private java.time.LocalDate practiceLastAttemptAt;

    @Column(columnDefinition = "TEXT")
    private String practiceFeedback;

    @Builder.Default
    private Boolean completed = false;    // true = все 3 этапа пройдены

    @Builder.Default
    private Integer pointsEarned = 0;     // сколько баллов начислено за урок

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
