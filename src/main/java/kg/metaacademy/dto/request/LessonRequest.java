package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LessonRequest {
    @NotBlank  private String title;
    @NotNull   private Integer orderIndex;
    private String practiceTask;
    private String practiceMode;   // "text" или "file"
    private String engLevel;        // для английских уроков: Beginner/Elementary/Pre-Intermediate/Intermediate
    private String aiQuestions;     // вопросы для AI-практики
    private Integer aiQuestionCount; // сколько вопросов задаёт ИИ (1-10)
}
