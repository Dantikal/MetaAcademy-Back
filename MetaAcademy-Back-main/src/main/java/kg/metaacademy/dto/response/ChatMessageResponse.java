package kg.metaacademy.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatMessageResponse {
    private Long   id;
    private String text;
    private String type;
    private Long   senderId;
    private String senderName;
    private String senderRole;
    private String senderAvatar;
    private Long   receiverId;
    private LocalDateTime sentAt;
}
