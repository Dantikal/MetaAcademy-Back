package kg.metaacademy.service.impl;

import kg.metaacademy.dto.response.LeaderboardEntry;
import kg.metaacademy.entity.Enrollment;
import kg.metaacademy.entity.User;
import kg.metaacademy.repository.EnrollmentRepository;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository       userRepo;
    private final EnrollmentRepository enrollmentRepo;

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getGlobal(Long currentUserId) {
        return toEntriesGlobal(userRepo.findTopStudentsByPoints(), currentUserId);
    }

    // ИСПРАВЛЕНО: добавляем progressPercent из Enrollment
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getByCourse(Long courseId, Long currentUserId) {
        List<User> users = userRepo.findStudentsByCourseOrderByPoints(courseId);

        // Загружаем enrollments одним запросом и строим Map<studentId, progressPercent>
        Map<Long, Integer> progressMap = enrollmentRepo
                .findByCourseIdAndActiveTrue(courseId)
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getStudent().getId(),
                        e -> e.getProgressPercent() != null ? e.getProgressPercent() : 0,
                        (a, b) -> a  // при дублях берём первый
                ));

        List<LeaderboardEntry> result = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);
        users.forEach(u -> result.add(LeaderboardEntry.builder()
                .userId(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .points(u.getPoints())
                .streak(u.getStreak())
                .completedLessons(u.getDaysOnPlatform())
                .progressPercent(progressMap.getOrDefault(u.getId(), 0))
                .rank(rank.getAndIncrement())
                .isCurrentUser(u.getId().equals(currentUserId))
                .build()));
        return result;
    }

    private List<LeaderboardEntry> toEntriesGlobal(List<User> users, Long currentUserId) {
        List<LeaderboardEntry> result = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);
        users.forEach(u -> result.add(LeaderboardEntry.builder()
                .userId(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .points(u.getPoints())
                .streak(u.getStreak())
                .completedLessons(u.getDaysOnPlatform())
                .progressPercent(0)
                .rank(rank.getAndIncrement())
                .isCurrentUser(u.getId().equals(currentUserId))
                .build()));
        return result;
    }
}
