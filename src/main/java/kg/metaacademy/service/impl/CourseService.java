package kg.metaacademy.service.impl;

import kg.metaacademy.dto.request.CourseRequest;
import kg.metaacademy.dto.response.CourseResponse;
import kg.metaacademy.entity.Course;
import kg.metaacademy.entity.User;
import kg.metaacademy.exception.BadRequestException;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.CourseRepository;
import kg.metaacademy.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepo;
    private final UserRepository   userRepo;

    // ── Чтение — все в транзакции чтобы lazy loading работал ─────────────────

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllActive() {
        return courseRepo.findByActiveTrue().stream()
                .filter(c -> {
                    // English курсы не показываем в каталоге — студент попадает туда через тест
                    String name = (c.getName() == null ? "" : c.getName()).toLowerCase();
                    return !name.contains("english") && !name.contains("англ");
                })
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAll() {
        return courseRepo.findAll().stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getByTeacher(Long teacherId) {
        return courseRepo.findByTeacherId(teacherId).stream()
                .map(this::toResponse).toList();
    }

    // ── Запись ────────────────────────────────────────────────────────────────

    @Transactional
    public CourseResponse create(CourseRequest req) {
        if (req.getTeacherId() == null)
            throw new BadRequestException("teacherId обязателен");

        User teacher = userRepo.findById(req.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Преподаватель не найден: id=" + req.getTeacherId()));

        Course course = Course.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .color(req.getColor())
                .icon(req.getIcon() != null ? req.getIcon() : "📚")
                .badge(req.getBadge())
                .durationMonths(req.getDurationMonths())
                .teacher(teacher)
                .active(true)
                .build();

        return toResponse(courseRepo.save(course));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest req) {
        Course course = findOrThrow(id);

        if (req.getName()           != null) course.setName(req.getName());
        if (req.getDescription()    != null) course.setDescription(req.getDescription());
        if (req.getPrice()          != null) course.setPrice(req.getPrice());
        if (req.getColor()          != null) course.setColor(req.getColor());
        if (req.getIcon()           != null) course.setIcon(req.getIcon());
        if (req.getBadge()          != null) course.setBadge(req.getBadge());
        if (req.getDurationMonths() != null) course.setDurationMonths(req.getDurationMonths());
        if (req.getTeacherId()      != null) {
            User teacher = userRepo.findById(req.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
            course.setTeacher(teacher);
        }

        return toResponse(courseRepo.save(course));
    }

    @Transactional
    public void toggleActive(Long id) {
        Course course = findOrThrow(id);
        course.setActive(!course.getActive());
        courseRepo.save(course);
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        courseRepo.deleteById(id);
    }

    // ── Создать 4 английских курса ───────────────────────────────────────────
    @Transactional
    public List<CourseResponse> setupEnglishCourses(List<Map<String, Object>> levels) {
        List<CourseResponse> result = new ArrayList<>();
        String[] engLevels = {"Beginner", "Elementary", "Pre-Intermediate", "Intermediate"};
        String[] icons = {"🌱", "📗", "📘", "🏆"};
        String[] colors = {"#0d9488", "#2563eb", "#7c3aed", "#d97706"};

        for (int i = 0; i < engLevels.length; i++) {
            String lvl = engLevels[i];
            final int idx = i;

            // Ищем нужный teacherId для этого уровня из переданных данных
            Long teacherId = levels.stream()
                    .filter(m -> lvl.equalsIgnoreCase(String.valueOf(m.get("level"))))
                    .map(m -> m.get("teacherId") instanceof Number n ? n.longValue() : null)
                    .filter(tid -> tid != null)
                    .findFirst()
                    .orElse(null);

            String courseName = "English " + lvl;
            // Пропускаем если уже существует
            boolean exists = courseRepo.findAll().stream()
                    .anyMatch(course -> courseName.equalsIgnoreCase(course.getName()));
            if (exists) {
                courseRepo.findAll().stream()
                        .filter(course -> courseName.equalsIgnoreCase(course.getName()))
                        .findFirst()
                        .ifPresent(existing -> result.add(toResponse(existing)));
                continue;
            }

            Course.CourseBuilder builder = Course.builder()
                    .name(courseName)
                    .description("English " + lvl + " — персональный курс английского языка")
                    .color(colors[idx])
                    .icon(icons[idx])
                    .price(java.math.BigDecimal.ZERO)
                    .durationMonths(3)
                    .active(true);

            if (teacherId != null) {
                final Long tid = teacherId;
                userRepo.findById(tid).ifPresent(builder::teacher);
            }

            Course saved = courseRepo.save(builder.build());
            result.add(toResponse(saved));
        }
        return result;
    }

    // ── Назначить преподавателя ───────────────────────────────────────────────
    @Transactional
    public CourseResponse assignTeacher(Long courseId, Long teacherId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));
        userRepo.findById(teacherId)
                .ifPresent(course::setTeacher);
        return toResponse(courseRepo.save(course));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Course findOrThrow(Long id) {
        return courseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден: " + id));
    }

    public CourseResponse toResponse(Course c) {
        // Безопасное получение количества уроков (lazy collection)
        int lessonsCount = 0;
        try {
            lessonsCount = c.getLessons() != null ? c.getLessons().size() : 0;
        } catch (Exception ignored) {}

        return CourseResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .price(c.getPrice())
                .color(c.getColor())
                .icon(c.getIcon())
                .badge(c.getBadge())
                .durationMonths(c.getDurationMonths())
                .totalLessons(lessonsCount)
                .active(c.getActive())
                .teacherName(c.getTeacher() != null
                        ? c.getTeacher().getFirstName() + " " + c.getTeacher().getLastName()
                        : null)
                .teacherAvatar(c.getTeacher() != null ? c.getTeacher().getAvatarUrl() : null)
                .teacherId(c.getTeacher() != null ? c.getTeacher().getId() : null)
                .studentsCount(courseRepo.countActiveStudents(c.getId()))
                .build();
    }
}
