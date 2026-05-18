package kg.metaacademy.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import kg.metaacademy.dto.request.UpdateProfileRequest;
import kg.metaacademy.dto.response.UserResponse;
import kg.metaacademy.entity.Enrollment;
import kg.metaacademy.entity.User;
import kg.metaacademy.enums.EnglishLevel;
import kg.metaacademy.enums.UserStatus;
import kg.metaacademy.exception.AlreadyExistsException;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.CourseRepository;
import kg.metaacademy.repository.EnrollmentRepository;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository       userRepo;
    private final Cloudinary           cloudinary;
    // БАГ 5: нужны для автозаписи на английский курс
    private final CourseRepository     courseRepo;
    private final EnrollmentRepository enrollmentRepo;

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return toResponse(userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден")));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> search(String query) {
        return userRepo.searchUsers(query).stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = findOrThrow(userId);

        if (req.getNickname() != null && !req.getNickname().equals(user.getNickname())) {
            if (userRepo.existsByNickname(req.getNickname()))
                throw new AlreadyExistsException("Никнейм уже занят");
            user.setNickname(req.getNickname());
        }
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()  != null) user.setLastName(req.getLastName());

        return toResponse(userRepo.save(user));
    }

    @Transactional
    public UserResponse uploadAvatar(Long userId, MultipartFile file) throws IOException {
        User user = findOrThrow(userId);

        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder",           "metaacademy/avatars",
                        "public_id",        "avatar_" + userId,
                        "overwrite",        true,
                        "transformation",   "w_300,h_300,c_fill,g_face"
                )
        );

        user.setAvatarUrl((String) result.get("secure_url"));
        return toResponse(userRepo.save(user));
    }

    // БАГ 5: после сохранения уровня автоматически создаём Enrollment на английский курс.
    // Раньше уровень сохранялся в БД, но курс в таблице enrollments не создавался,
    // поэтому после перезагрузки страницы курс исчезал из списка студента.
    @Transactional
    public void setEnglishLevel(Long userId, String level) {
        User user = findOrThrow(userId);
        // Нормализуем: "Pre-Intermediate" → "PRE_INTERMEDIATE"
        String enumName = level.toUpperCase()
                .replace("-", "_")
                .replace(" ", "_")
                .replace("__", "_");
        user.setEnglishLevel(EnglishLevel.valueOf(enumName));
        userRepo.save(user);

        // Ищем активный английский курс КОНКРЕТНОГО уровня по названию
        // Пример: "English Beginner", "English Elementary" и т.д.
        String levelLower = level.toLowerCase();
        courseRepo.findAll().stream()
                .filter(course -> {
                    String name = (course.getName() == null ? "" : course.getName()).toLowerCase();
                    boolean isEng  = name.contains("english") || name.contains("англ");
                    boolean isLevel = name.contains(levelLower);
                    // Fallback: если нет курса с уровнем в названии — берём любой английский
                    return Boolean.TRUE.equals(course.getActive()) && isEng && isLevel;
                })
                .findFirst()
                .or(() -> courseRepo.findAll().stream()
                        .filter(course -> {
                            String name = (course.getName() == null ? "" : course.getName()).toLowerCase();
                            return Boolean.TRUE.equals(course.getActive())
                                    && (name.contains("english") || name.contains("англ"));
                        })
                        .findFirst()
                )
                .ifPresent(engCourse -> {
                    if (!enrollmentRepo.existsByStudentIdAndCourseId(userId, engCourse.getId())) {
                        LocalDateTime now = LocalDateTime.now();
                        enrollmentRepo.save(Enrollment.builder()
                                .student(user)
                                .course(engCourse)
                                .purchasedAt(now)
                                .expiresAt(now.plusDays(30))
                                .active(true)
                                .build());
                    }
                });
    }

    @Transactional
    public void toggleBlock(Long userId) {
        User user = findOrThrow(userId);
        user.setStatus(user.getStatus() == UserStatus.ACTIVE
                ? UserStatus.BLOCKED : UserStatus.ACTIVE);
        userRepo.save(user);
    }

    @Transactional
    public void delete(Long userId) {
        findOrThrow(userId);
        userRepo.deleteById(userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // ── Обновить стрик при ежедневном посещении ─────────────────────────────
    @Transactional
    public void updateStreakOnVisit(Long userId) {
        User user = findOrThrow(userId);
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate last  = user.getLastActiveDate();

        if (last == null) {
            // Первый вход
            user.setStreak(1);
            user.setLastActiveDate(today);
            userRepo.save(user);
            return;
        }

        if (last.isEqual(today)) {
            // Уже заходил сегодня — ничего не делаем
            return;
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(last, today);

        if (daysBetween == 1) {
            // Заходил вчера — продолжаем стрик
            int newStreak = (user.getStreak() == null ? 0 : user.getStreak()) + 1;
            user.setStreak(newStreak);
            user.setLastActiveDate(today);

            // +5 баллов за каждые 10 дней подряд
            if (newStreak % 10 == 0) {
                user.setPoints((user.getPoints() == null ? 0 : user.getPoints()) + 5);
            }
        } else {
            // Пропустил день(и) — обнуляем стрик
            user.setStreak(1);
            user.setLastActiveDate(today);
        }

        userRepo.save(user);
    }

    private User findOrThrow(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + id));
    }

    public UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole().name())
                .status(u.getStatus().name())
                .englishLevel(u.getEnglishLevel() != null ? u.getEnglishLevel().name() : null)
                .points(u.getPoints())
                .streak(u.getStreak())
                .daysOnPlatform(u.getDaysOnPlatform())
                .age(u.getAge())
                .region(u.getRegion())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
