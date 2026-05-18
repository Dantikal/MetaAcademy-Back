package kg.metaacademy.repository;
import kg.metaacademy.entity.User;
import kg.metaacademy.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role='STUDENT' ORDER BY u.points DESC")
    List<User> findTopStudentsByPoints();

    // БАГ 9: только студенты с активным enrollment именно на этот курс.
    // DISTINCT убирает дублирование если у студента несколько записей на один курс.
    @Query("""
        SELECT DISTINCT u FROM User u JOIN u.enrollments e
        WHERE e.course.id = :courseId
          AND e.active = true
          AND u.role = 'STUDENT'
        ORDER BY u.points DESC
        """)
    List<User> findStudentsByCourseOrderByPoints(@Param("courseId") Long courseId);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(u.email)     LIKE LOWER(CONCAT('%',:q,'%'))
        """)
    List<User> searchUsers(@Param("q") String query);
}
