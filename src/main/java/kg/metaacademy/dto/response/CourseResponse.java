package kg.metaacademy.dto.response;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseResponse {
    private Long   id;
    private String name;
    private String description;
    private BigDecimal price;
    private String color;
    private String icon;
    private String badge;
    private Integer durationMonths;
    private Integer totalLessons;
    private Boolean active;
    private String  teacherName;
    private String  teacherAvatar;
    private Long    teacherId;
    private Long    studentsCount;
}
