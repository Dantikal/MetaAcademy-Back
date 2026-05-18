package kg.metaacademy.scheduler;

import kg.metaacademy.service.impl.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentScheduler {

    private final EnrollmentService enrollmentService;

    /** Каждый час проверяем истёкшие подписки */
    @Scheduled(cron = "0 0 * * * *")
    public void deactivateExpiredEnrollments() {
        log.debug("Checking expired enrollments...");
        enrollmentService.deactivateExpired();
    }
}
