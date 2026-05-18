package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class GroupCallRequest {
    @NotNull private LocalDate callDate;
    @NotNull private LocalTime callTime;
    private String zoomLink;
}
