package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotBlank private String text;
    private Long receiverId;  // null для GROUP
}
