package kg.metaacademy.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private Long   id;
    private String firstName;
    private String lastName;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String status;
    private String englishLevel;
    private Integer points;
    private Integer streak;
    private Integer daysOnPlatform;
    private Integer age;       // возраст
    private String  region;    // регион проживания
    private LocalDateTime createdAt;
}
