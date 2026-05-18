package kg.metaacademy.repository;
import kg.metaacademy.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByActiveTrue();
    List<Course> findByTeacherId(Long teacherId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id=:id AND e.active=true")
    Long countActiveStudents(@Param("id") Long courseId);
}
