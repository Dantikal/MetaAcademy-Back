package kg.metaacademy.repository;
import kg.metaacademy.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByStudentIdAndLessonId(Long studentId, Long lessonId);
    List<LessonProgress> findByStudentIdAndLessonCourseId(Long studentId, Long courseId);

    @Query("""
        SELECT COUNT(lp) FROM LessonProgress lp
        WHERE lp.student.id=:sid AND lp.lesson.course.id=:cid AND lp.completed=true
        """)
    Long countCompleted(@Param("sid") Long studentId, @Param("cid") Long courseId);
}
