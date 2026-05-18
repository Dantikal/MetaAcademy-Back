package kg.metaacademy.dto.response;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QuizQuestionResponse {
    private Long   id;
    private String questionText;
    private Integer orderIndex;
    private List<QuizOptionResponse> options;
}
