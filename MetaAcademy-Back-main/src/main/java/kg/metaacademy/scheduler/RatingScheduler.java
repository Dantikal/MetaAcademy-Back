package kg.metaacademy.scheduler;

import kg.metaacademy.entity.Course;
import kg.metaacademy.entity.Enrollment;
import kg.metaacademy.entity.MonthlyRating;
import kg.metaacademy.entity.User;
import kg.metaacademy.enums.Role;
import kg.metaacademy.repository.CourseRepository;
import kg.metaacademy.repository.MonthlyRatingRepository;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatingScheduler {

    private final UserRepository          userRepo;
    private final CourseRepository        courseRepo;
    private final MonthlyRatingRepository ratingRepo;

    /**
     * 1-е число каждого месяца в 00:00 — сохраняем рейтинг и обнуляем баллы.
     * cron: "0 0 0 1 * *"
     */
    @Scheduled(cron = "${gamification.rating-reset-cron}")
    @Transactional
    public void resetMonthlyRating() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int year  = yesterday.getYear();
        int month = yesterday.getMonthValue();

        log.info("Saving monthly rating for {}/{}", year, month);

        List<Course> courses = courseRepo.findByActiveTrue();

        courses.forEach(course -> {
            course.getEnrollments().stream()
                    .filter(Enrollment::getActive)
                    .map(Enrollment::getStudent)
                    .filter(u -> u.getRole() == Role.STUDENT)
                    .max((a, b) -> Integer.compare(
                            a.getPoints() != null ? a.getPoints() : 0,
                            b.getPoints() != null ? b.getPoints() : 0))
                    .ifPresent(topStudent -> {
                        MonthlyRating mr = MonthlyRating.builder()
                                .user(topStudent)
                                .course(course)
                                .year(year)
                                .month(month)
                                .totalPoints(topStudent.getPoints())
                                .groupRank(1)
                                .prizeAwarded(true)
                                .build();
                        ratingRepo.save(mr);
                    });
        });

        // Обнуляем баллы всех студентов
        userRepo.findByRole(Role.STUDENT).forEach(u -> {
            u.setPoints(0);
            userRepo.save(u);
        });

        log.info("Monthly rating reset complete for {}/{}", year, month);
    }
}
