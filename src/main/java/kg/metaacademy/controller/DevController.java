package kg.metaacademy.controller;

import kg.metaacademy.entity.User;
import kg.metaacademy.enums.Role;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * ТОЛЬКО ДЛЯ РАЗРАБОТКИ — удали или отключи перед деплоем!
 * Позволяет создавать тестовых пользователей через API.
 */
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    /**
     * GET http://localhost:8080/api/dev/create-admin?password=admin20260316
     * Создаёт или обновляет пароль админа
     */
    @GetMapping("/create-admin")
    public ResponseEntity<Map<String,String>> createAdmin(
            @RequestParam(defaultValue = "admin123") String password) {

        User admin = userRepo.findByEmail("admin@metaacademy.kg")
                .orElse(User.builder()
                        .email("admin@metaacademy.kg")
                        .firstName("Администратор")
                        .lastName("")
                        .nickname("admin_meta")
                        .role(Role.ADMIN)
                        .build());

        admin.setPassword(encoder.encode(password));
        admin.setLastVisitDate(LocalDate.now());
        userRepo.save(admin);

        return ResponseEntity.ok(Map.of(
                "status", "created",
                "email",  "admin@metaacademy.kg",
                "password", password,
                "role", "ADMIN"
        ));
    }

    /**
     * GET http://localhost:8080/api/dev/create-teacher?password=teacher123
     * Создаёт или обновляет пароль преподавателя
     */
    @GetMapping("/create-teacher")
    public ResponseEntity<Map<String,String>> createTeacher(
            @RequestParam(defaultValue = "teacher123") String password) {

        User teacher = userRepo.findByEmail("teacher@metaacademy.kg")
                .orElse(User.builder()
                        .email("teacher@metaacademy.kg")
                        .firstName("Алексей")
                        .lastName("Иванов")
                        .nickname("teacher_ivanov")
                        .role(Role.TEACHER)
                        .build());

        teacher.setPassword(encoder.encode(password));
        teacher.setLastVisitDate(LocalDate.now());
        userRepo.save(teacher);

        return ResponseEntity.ok(Map.of(
                "status",   "created",
                "email",    "teacher@metaacademy.kg",
                "password", password,
                "role",     "TEACHER"
        ));
    }
}
