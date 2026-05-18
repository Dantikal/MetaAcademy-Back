package kg.metaacademy.controller;

import jakarta.validation.Valid;
import kg.metaacademy.dto.request.SendMessageRequest;
import kg.metaacademy.dto.response.ChatMessageResponse;
import kg.metaacademy.security.SecurityUtils;
import kg.metaacademy.service.impl.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService   chatService;
    private final SecurityUtils security;

    // ИСПРАВЛЕНО: передаём requesterId чтобы ChatService мог проверить доступ
    @GetMapping("/group/{courseId}")
    public ResponseEntity<List<ChatMessageResponse>> group(@PathVariable Long courseId) {
        return ResponseEntity.ok(
                chatService.getGroupMessages(courseId, security.getCurrentUserId()));
    }

    @GetMapping("/dm/{courseId}/{userId2}")
    public ResponseEntity<List<ChatMessageResponse>> dm(@PathVariable Long courseId,
                                                         @PathVariable Long userId2) {
        return ResponseEntity.ok(
                chatService.getDirectMessages(courseId, security.getCurrentUserId(), userId2));
    }

    @PostMapping("/send/{courseId}")
    public ResponseEntity<ChatMessageResponse> send(@PathVariable Long courseId,
                                                     @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(
                chatService.send(security.getCurrentUserId(), courseId, req));
    }
}
