package kg.metaacademy.dto.response;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssignmentResponse {
    private Long   id;
    private String title;
    private String description;
    private LocalDate deadline;
    private String type;
    private String targetStudentName;
    private LocalDateTime createdAt;
    private List<SubmissionResponse> submissions;
}
