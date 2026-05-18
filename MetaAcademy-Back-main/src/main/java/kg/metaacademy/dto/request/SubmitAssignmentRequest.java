package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubmitAssignmentRequest {
    @NotBlank private String githubUrl;
}
