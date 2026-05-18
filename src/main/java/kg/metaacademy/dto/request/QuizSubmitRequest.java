package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Map;

@Data
public class QuizSubmitRequest {
    @NotNull private Long lessonId;
    // Map<questionId, selectedOptionId>
    @NotEmpty private Map<Long, Long> answers;
}
