package kg.metaacademy.dto.response;
import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private Long   userId;
    private String email;
    private String firstName;
    private String lastName;
    private String nickname;
    private String avatarUrl;
    private String englishLevel;
}
