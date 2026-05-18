package kg.metaacademy.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EnrollmentResponse {
    private Long   courseId;
    private String courseName;
    private String courseColor;
    private String courseIcon;
    private String teacherName;
    private LocalDateTime purchasedAt;
    private LocalDateTime expiresAt;
    private Integer daysLeft;
    private Boolean active;
    private Integer completedLessons;
    private Integer totalLessons;
    private Integer progressPercent;
    private Boolean isEng;
}
