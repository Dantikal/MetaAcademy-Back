package kg.metaacademy.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "monthly_ratings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MonthlyRating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false) private Integer year;
    @Column(nullable = false) private Integer month;
    @Column(nullable = false) private Integer totalPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private Integer groupRank;

    @Builder.Default
    private Boolean prizeAwarded = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
