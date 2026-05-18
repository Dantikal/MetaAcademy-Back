package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import kg.metaacademy.enums.AssignmentType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AssignmentRequest {
    @NotBlank  private String title;
    @NotBlank  private String description;
    @NotNull   private LocalDate deadline;
    @NotNull   private AssignmentType type;
    private Long targetStudentId;  // только для INDIVIDUAL
    @NotNull   private Long courseId;
}
