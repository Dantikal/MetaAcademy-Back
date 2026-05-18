package kg.metaacademy.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminStudentResponse {
    // Студент
    private Long   studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String nickname;
    private String region;
    private Integer age;
    private String status;       // ACTIVE / BLOCKED
    private Integer points;
    private Integer streak;
    private LocalDateTime registeredAt;

    // Курс
    private Long   courseId;
    private String courseName;
    private String courseIcon;
    private String courseColor;
    private String teacherName;

    // Прогресс
    private Integer completedLessons;
    private Integer totalLessons;
    private Integer progressPercent;
    private Boolean isGraduated;   // 100% пройден
    private Boolean isActive;      // enrolled + активный + не истёк срок
    private Boolean isArchived;    // истёк срок / inactive
    private LocalDateTime purchasedAt;
    private LocalDateTime expiresAt;
    private Integer daysLeft;
}
