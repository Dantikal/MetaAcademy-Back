package kg.metaacademy.repository;
import kg.metaacademy.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    @Modifying
    @Query(value = "DELETE FROM quiz_options WHERE question_id IN " +
                   "(SELECT id FROM quiz_questions WHERE lesson_id = :lessonId)", nativeQuery = true)
    void deleteOptionsByLessonId(@Param("lessonId") Long lessonId);

    @Modifying
    @Query("DELETE FROM QuizQuestion q WHERE q.lesson.id = :lessonId")
    void deleteByLessonId(@Param("lessonId") Long lessonId);
}
