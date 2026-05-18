package kg.metaacademy.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubmissionResponse {
    private Long   studentId;
    private String studentName;
    private String githubUrl;
    private String fileName;
    private String fileType;
    private String status;
    private Integer pointsAwarded;
    private Integer attemptsToday;
    private String feedback;
    private LocalDateTime submittedAt;
}
