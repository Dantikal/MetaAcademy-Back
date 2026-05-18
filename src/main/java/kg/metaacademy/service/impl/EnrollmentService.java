package kg.metaacademy.service.impl;

import kg.metaacademy.dto.response.EnrollmentResponse;
import kg.metaacademy.entity.Course;
import kg.metaacademy.entity.Enrollment;
import kg.metaacademy.entity.User;
import kg.metaacademy.exception.AlreadyExistsException;
import kg.metaacademy.exception.BadRequestException;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.CourseRepository;
import kg.metaacademy.repository.EnrollmentRepository;
import kg.metaacademy.repository.LessonRepository;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepo;
    private final CourseRepository     courseRepo;
    private final UserRepository       userRepo;
    private final LessonRepository     lessonRepo;

    // Купить курс ──────────────────────────────────────────────────────────────
    @Transactional
    public EnrollmentResponse purchase(Long studentId, Long courseId) {
        if (enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId))
            throw new AlreadyExistsException("Вы уже записаны на этот курс");

        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));

        LocalDateTime now = LocalDateTime.now();
        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .purchasedAt(now)
                .expiresAt(now.plusDays(30))
                .active(true)
                .build();

        return toResponse(enrollmentRepo.save(enrollment), course);
    }

    // Продлить доступ (+30 дней) ───────────────────────────────────────────────
    @Transactional
    public EnrollmentResponse renew(Long studentId, Long courseId) {
        Enrollment e = enrollmentRepo.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Запись на курс не найдена"));

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));

        LocalDateTime now = LocalDateTime.now();
        // Продлеваем от текущего expiresAt или от сейчас (если уже истёк)
        LocalDateTime base = e.getExpiresAt().isAfter(now) ? e.getExpiresAt() : now;
        e.setExpiresAt(base.plusDays(30));
        e.setPurchasedAt(now);
        e.setActive(true);

        return toResponse(enrollmentRepo.save(e), course);
    }

    // Список активных курсов студента ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(Long studentId) {
        return enrollmentRepo.findByStudentIdAndActiveTrue(studentId).stream()
                .map(e -> toResponse(e, e.getCourse()))
                .toList();
    }

    // Проверить доступ ────────────────────────────────────────────────────────
    public boolean hasAccess(Long studentId, Long courseId) {
        return enrollmentRepo.findByStudentIdAndCourseId(studentId, courseId)
                .map(Enrollment::hasAccess)
                .orElse(false);
    }

    // Проверяет только наличие записи (без проверки срока)
    public boolean isEnrolled(Long studentId, Long courseId) {
        return enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId);
    }

    // Обновить прогресс (вызывается из ProgressService) ───────────────────────
    @Transactional
    public void updateProgress(Long studentId, Long courseId, int completed) {
        Enrollment e = enrollmentRepo.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Запись не найдена"));

        long total = lessonRepo.countByCourseId(courseId);
        e.setCompletedLessons(completed);
        e.setProgressPercent(total > 0 ? (int) Math.round(completed * 100.0 / total) : 0);
        enrollmentRepo.save(e);
    }

    // Деактивировать истёкшие (вызывается из планировщика) ────────────────────
    @Transactional
    public void deactivateExpired() {
        List<Enrollment> expired = enrollmentRepo.findExpired(LocalDateTime.now());
        expired.forEach(e -> e.setActive(false));
        enrollmentRepo.saveAll(expired);
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private EnrollmentResponse toResponse(Enrollment e, Course c) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), e.getExpiresAt());
        return EnrollmentResponse.builder()
                .courseId(c.getId())
                .courseName(c.getName())
                .courseColor(c.getColor())
                .courseIcon(c.getIcon())
                .teacherName(c.getTeacher() != null
                        ? c.getTeacher().getFirstName() + " " + c.getTeacher().getLastName() : null)
                .purchasedAt(e.getPurchasedAt())
                .expiresAt(e.getExpiresAt())
                .daysLeft((int) Math.max(0, daysLeft))
                .active(e.hasAccess())
                .completedLessons(e.getCompletedLessons())
                .totalLessons(c.getLessons() != null ? c.getLessons().size() : 0)
                .progressPercent(e.getProgressPercent())
                .isEng(c.getName() != null && (c.getName().toLowerCase().contains("english") || c.getName().toLowerCase().contains("англ")))
                .build();
    }

    // ── Все студенты для AdminPanel ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<kg.metaacademy.dto.response.AdminStudentResponse> getAllStudentsForAdmin() {
        return enrollmentRepo.findAllWithDetails().stream()
                .filter(e -> e.getCourse() != null && e.getStudent() != null)
                .filter(e -> {
                    // Исключаем английские курсы из этого списка (они в отдельной вкладке)
                    String name = e.getCourse().getName() == null ? "" : e.getCourse().getName().toLowerCase();
                    return !name.contains("english") && !name.contains("англ");
                })
                .map(e -> {
                    var student = e.getStudent();
                    var course  = e.getCourse();
                    int pct     = e.getProgressPercent() != null ? e.getProgressPercent() : 0;
                    int total   = (course != null && course.getLessons() != null) ? course.getLessons().size() : 0;
                    boolean graduated = pct >= 100 && total > 0;
                    boolean active    = Boolean.TRUE.equals(e.getActive())
                            && e.getExpiresAt() != null
                            && java.time.LocalDateTime.now().isBefore(e.getExpiresAt());
                    boolean archived  = !active && !graduated;

                    long daysLeft = 0;
                    if (e.getExpiresAt() != null) {
                        daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                                java.time.LocalDateTime.now(), e.getExpiresAt());
                    }

                    return kg.metaacademy.dto.response.AdminStudentResponse.builder()
                            .studentId(student.getId())
                            .firstName(student.getFirstName())
                            .lastName(student.getLastName())
                            .email(student.getEmail())
                            .nickname(student.getNickname())
                            .region(student.getRegion())
                            .age(student.getAge())
                            .status(student.getStatus() != null ? student.getStatus().name() : "ACTIVE")
                            .points(student.getPoints() != null ? student.getPoints() : 0)
                            .streak(student.getStreak() != null ? student.getStreak() : 0)
                            .registeredAt(student.getCreatedAt())
                            .courseId(course.getId())
                            .courseName(course.getName())
                            .courseIcon(course.getIcon())
                            .courseColor(course.getColor())
                            .teacherName(course.getTeacher() != null
                                    ? course.getTeacher().getFirstName() + " " + course.getTeacher().getLastName()
                                    : "—")
                            .completedLessons(e.getCompletedLessons() != null ? e.getCompletedLessons() : 0)
                            .totalLessons(total)
                            .progressPercent(pct)
                            .isGraduated(graduated)
                            .isActive(active)
                            .isArchived(archived)
                            .purchasedAt(e.getPurchasedAt())
                            .expiresAt(e.getExpiresAt())
                            .daysLeft((int) Math.max(0, daysLeft))
                            .build();
                })
                .toList();
    }
}
