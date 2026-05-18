package kg.metaacademy.dto.response;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LessonResponse {
    private Long   id;
    private String title;
    private Integer orderIndex;
    private String videoUrl;
    private String practiceTask;
    private String practiceMode;    // "text" или "file"
    private String engLevel;        // для английских уроков
    private String aiQuestions;     // вопросы для AI-практики
    private Integer aiQuestionCount;
    private List<QuizQuestionResponse> quizQuestions;
    // Прогресс студента
    private Boolean videoWatched;
    private Boolean quizPassed;
    private Boolean practiceDone;
    private Boolean completed;
    private Integer quizAttempts;
}
