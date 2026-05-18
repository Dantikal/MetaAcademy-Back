package kg.metaacademy.repository;
import kg.metaacademy.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.course.id=:cid
          AND (a.type='GROUP' OR a.targetStudent.id=:sid)
        ORDER BY a.createdAt DESC
        """)
    List<Assignment> findForStudent(@Param("cid") Long courseId, @Param("sid") Long studentId);
}
