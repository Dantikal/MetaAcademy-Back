package kg.metaacademy.dto.response;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupCallResponse {
    private Long   id;
    private LocalDate callDate;
    private LocalTime callTime;
    private String zoomLink;
    private String courseName;
    private Boolean isLive;    // идёт ли прямо сейчас
}
