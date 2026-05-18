package kg.metaacademy.repository;
import kg.metaacademy.entity.MonthlyRating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MonthlyRatingRepository extends JpaRepository<MonthlyRating, Long> {
    List<MonthlyRating> findByYearAndMonthOrderByTotalPointsDesc(int year, int month);
    Optional<MonthlyRating> findByUserIdAndYearAndMonth(Long userId, int year, int month);
}
