package kg.metaacademy.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class SaveQuizRequest {

    @NotEmpty
    private List<Question> questions;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Question {
        @NotBlank private String questionText;
        private Integer orderIndex;
        @NotEmpty @Size(min=2, max=6) private List<Option> options;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Option {
        @NotBlank  private String  optionText;
        @NotNull   private Boolean isCorrect;
        private Integer orderIndex;
    }
}
