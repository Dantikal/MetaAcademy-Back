package kg.metaacademy.repository;
import kg.metaacademy.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);
    long countByCourseId(Long courseId);
    boolean existsByCourseIdAndOrderIndex(Long courseId, int orderIndex);
}
