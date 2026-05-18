package kg.metaacademy.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email @NotBlank private String email;
    @NotBlank @Size(min = 6) private String password;
    private String nickname;

    @Min(10) @Max(100)
    private Integer age;      // возраст

    private String region;    // регион проживания в Кыргызстане
}
