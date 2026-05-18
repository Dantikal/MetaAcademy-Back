package kg.metaacademy.repository;
import kg.metaacademy.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
