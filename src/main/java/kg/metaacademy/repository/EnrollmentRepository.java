package kg.metaacademy.repository;
import kg.metaacademy.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    List<Enrollment> findByStudentIdAndActiveTrue(Long studentId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    // Все enrollments для adminPanel
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Enrollment e JOIN FETCH e.student JOIN FETCH e.course ORDER BY e.purchasedAt DESC")
    List<Enrollment> findAllWithDetails();

    // ДОБАВЛЕНО: для получения прогресса всех студентов курса
    List<Enrollment> findByCourseIdAndActiveTrue(Long courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.active=true AND e.expiresAt < :now")
    List<Enrollment> findExpired(@Param("now") LocalDateTime now);
}
