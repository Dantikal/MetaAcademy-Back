package kg.metaacademy.repository;
import kg.metaacademy.entity.GroupCall;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface GroupCallRepository extends JpaRepository<GroupCall, Long> {
    List<GroupCall> findByCourseIdOrderByCallDateAscCallTimeAsc(Long courseId);
    List<GroupCall> findByCourseIdAndCallDateGreaterThanEqual(Long courseId, LocalDate from);
}
