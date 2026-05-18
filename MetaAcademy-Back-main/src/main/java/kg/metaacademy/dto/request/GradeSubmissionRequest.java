package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GradeSubmissionRequest {
    @NotNull @Min(3) @Max(10) private Integer points;  // 3, 5 или 10
}
