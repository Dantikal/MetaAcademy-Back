package kg.metaacademy.websocket;

import kg.metaacademy.dto.request.SendMessageRequest;
import kg.metaacademy.dto.response.ChatMessageResponse;
import kg.metaacademy.repository.UserRepository;
import kg.metaacademy.service.impl.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService           chatService;
    private final UserRepository        userRepo;
    private final SimpMessagingTemplate broker;

    /**
     * Групповой чат курса.
     * Фронт отправляет: /app/chat.group.{courseId}
     * Все подписчики получают: /topic/chat.{courseId}
     */
    @MessageMapping("/chat.group.{courseId}")
    @SendTo("/topic/chat.{courseId}")
    public ChatMessageResponse groupMessage(@DestinationVariable Long courseId,
                                             @Payload SendMessageRequest req,
                                             Principal principal) {
        Long senderId = userRepo.findByEmail(principal.getName())
                .orElseThrow().getId();
        return chatService.send(senderId, courseId, req);
    }

    /**
     * Личное сообщение (DM).
     * Фронт отправляет: /app/chat.dm.{courseId}
     * Получатель получает: /user/{receiverId}/queue/messages
     */
    @MessageMapping("/chat.dm.{courseId}")
    public void directMessage(@DestinationVariable Long courseId,
                               @Payload SendMessageRequest req,
                               Principal principal) {
        Long senderId = userRepo.findByEmail(principal.getName())
                .orElseThrow().getId();

        ChatMessageResponse msg = chatService.send(senderId, courseId, req);

        // Доставляем отправителю (эхо)
        broker.convertAndSendToUser(principal.getName(), "/queue/messages", msg);

        // Доставляем получателю
        if (req.getReceiverId() != null) {
            String receiverEmail = userRepo.findById(req.getReceiverId())
                    .orElseThrow().getEmail();
            broker.convertAndSendToUser(receiverEmail, "/queue/messages", msg);
        }
    }
}
