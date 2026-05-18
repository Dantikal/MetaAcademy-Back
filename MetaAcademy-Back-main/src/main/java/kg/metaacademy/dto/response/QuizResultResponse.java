package kg.metaacademy.dto.response;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QuizResultResponse {
    private Boolean passed;        // 100% ли правильных
    private Integer correctCount;
    private Integer totalCount;
    private Integer attempt;
    private Integer pointsEarned;  // начислено (только при passed=true)
}
