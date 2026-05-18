package kg.metaacademy.dto.response;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaderboardEntry {
    private Long    userId;
    private String  firstName;
    private String  lastName;
    private String  nickname;
    private String  avatarUrl;
    private Integer points;
    private Integer streak;
    private Integer completedLessons;
    private Integer progressPercent;  // ДОБАВЛЕНО: прогресс по курсу
    private Integer rank;
    private Boolean isCurrentUser;
}
