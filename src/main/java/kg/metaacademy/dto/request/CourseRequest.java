package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CourseRequest {
    @NotBlank  private String name;
    private String description;
    @NotNull   private BigDecimal price;
    private String color;
    private String icon;
    private String badge;
    private Integer durationMonths;
    @NotNull   private Long teacherId;
}
