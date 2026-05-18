package kg.metaacademy.repository;
import kg.metaacademy.entity.ChatMessage;
import kg.metaacademy.enums.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByCourseIdAndTypeOrderBySentAtAsc(Long courseId, MessageType type);

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.course.id=:cid AND m.type='DIRECT'
          AND ((m.sender.id=:u1 AND m.receiver.id=:u2)
            OR (m.sender.id=:u2 AND m.receiver.id=:u1))
        ORDER BY m.sentAt ASC
        """)
    List<ChatMessage> findDM(@Param("cid") Long courseId,
                             @Param("u1") Long user1,
                             @Param("u2") Long user2);
}
