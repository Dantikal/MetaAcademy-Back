package kg.metaacademy.controller;

import kg.metaacademy.security.SecurityUtils;
import kg.metaacademy.service.impl.EnglishChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/english-chat")
@RequiredArgsConstructor
public class EnglishChatController {

    private final EnglishChatService chatService;
    private final SecurityUtils      security;

    /**
     * Отправить сообщение студента и получить ответ AI.
     * POST /api/english-chat/{lessonId}/message
     * Body: { "message": "..." }
     */
    @PostMapping("/{lessonId}/message")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long lessonId,
            @RequestBody Map<String, String> body) {
        Long studentId = security.getCurrentUserId();
        String userMessage = body.getOrDefault("message", "");
        return ResponseEntity.ok(chatService.chat(studentId, lessonId, userMessage));
    }

    /**
     * Получить историю чата студента для урока.
     * GET /api/english-chat/{lessonId}/history
     */
    @GetMapping("/{lessonId}/history")
    public ResponseEntity<List<Map<String, String>>> getHistory(@PathVariable Long lessonId) {
        Long studentId = security.getCurrentUserId();
        return ResponseEntity.ok(chatService.getHistory(studentId, lessonId));
    }

    /**
     * Сбросить сессию чата (начать заново).
     * DELETE /api/english-chat/{lessonId}/reset
     */
    @DeleteMapping("/{lessonId}/reset")
    public ResponseEntity<Void> reset(@PathVariable Long lessonId) {
        Long studentId = security.getCurrentUserId();
        chatService.reset(studentId, lessonId);
        return ResponseEntity.ok().build();
    }
}
