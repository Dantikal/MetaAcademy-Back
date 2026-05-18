package kg.metaacademy.dto.response;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QuizOptionResponse {
    private Long    id;
    private String  optionText;
    private Integer orderIndex;
    // isCorrect возвращается ТОЛЬКО после сдачи теста, не во время
    private Boolean isCorrect;
}
