package kg.metaacademy.service.impl;

import kg.metaacademy.dto.request.SendMessageRequest;
import kg.metaacademy.dto.response.ChatMessageResponse;
import kg.metaacademy.entity.ChatMessage;
import kg.metaacademy.entity.Course;
import kg.metaacademy.entity.User;
import kg.metaacademy.enums.MessageType;
import kg.metaacademy.enums.Role;
import kg.metaacademy.exception.AccessDeniedException;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.ChatMessageRepository;
import kg.metaacademy.repository.CourseRepository;
import kg.metaacademy.repository.EnrollmentRepository;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatRepo;
    private final UserRepository        userRepo;
    private final CourseRepository      courseRepo;
    private final EnrollmentRepository  enrollmentRepo;

    // ── Отправить сообщение ───────────────────────────────────────────────────
    @Transactional
    public ChatMessageResponse send(Long senderId, Long courseId, SendMessageRequest req) {
        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));

        // ИСПРАВЛЕНО: студент может писать только в чат курса на который записан
        if (sender.getRole() == Role.STUDENT) {
            boolean hasEnrollment = enrollmentRepo
                    .findByStudentIdAndCourseId(senderId, courseId)
                    .map(e -> Boolean.TRUE.equals(e.getActive()))
                    .orElse(false);
            if (!hasEnrollment) {
                throw new AccessDeniedException("Нет доступа к чату — сначала купите курс");
            }
        }

        // ИСПРАВЛЕНО: преподаватель может писать только в чат своего курса
        if (sender.getRole() == Role.TEACHER) {
            boolean isCourseTeacher = course.getTeacher() != null
                    && course.getTeacher().getId().equals(senderId);
            if (!isCourseTeacher) {
                throw new AccessDeniedException("Это не ваш курс");
            }
        }

        MessageType type = req.getReceiverId() == null ? MessageType.GROUP : MessageType.DIRECT;
        User receiver = null;
        if (req.getReceiverId() != null) {
            receiver = userRepo.findById(req.getReceiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Получатель не найден"));
        }

        ChatMessage msg = ChatMessage.builder()
                .text(req.getText())
                .type(type)
                .sender(sender)
                .course(course)
                .receiver(receiver)
                .build();

        return toResponse(chatRepo.save(msg));
    }

    // ── Групповой чат курса ───────────────────────────────────────────────────
    // ИСПРАВЛЕНО: проверяем что запрашивающий имеет доступ к курсу
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getGroupMessages(Long courseId, Long requesterId) {
        checkChatAccess(requesterId, courseId);
        return chatRepo.findByCourseIdAndTypeOrderBySentAtAsc(courseId, MessageType.GROUP)
                .stream().map(this::toResponse).toList();
    }

    // ── Личные сообщения ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getDirectMessages(Long courseId, Long u1, Long u2) {
        checkChatAccess(u1, courseId);
        return chatRepo.findDM(courseId, u1, u2)
                .stream().map(this::toResponse).toList();
    }

    // ── Проверка доступа к чату ───────────────────────────────────────────────
    private void checkChatAccess(Long userId, Long courseId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        // Админ — доступ везде
        if (user.getRole() == Role.ADMIN) return;

        // Преподаватель — только свои курсы
        if (user.getRole() == Role.TEACHER) {
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));
            if (course.getTeacher() == null || !course.getTeacher().getId().equals(userId)) {
                throw new AccessDeniedException("Это не ваш курс");
            }
            return;
        }

        // Студент — только курсы на которые записан
        boolean hasEnrollment = enrollmentRepo
                .findByStudentIdAndCourseId(userId, courseId)
                .map(e -> Boolean.TRUE.equals(e.getActive()))
                .orElse(false);
        if (!hasEnrollment) {
            throw new AccessDeniedException("Нет доступа к чату — сначала купите курс");
        }
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .text(m.getText())
                .type(m.getType().name())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFirstName() + " " + m.getSender().getLastName())
                .senderRole(m.getSender().getRole().name())
                .senderAvatar(m.getSender().getAvatarUrl())
                .receiverId(m.getReceiver() != null ? m.getReceiver().getId() : null)
                .sentAt(m.getSentAt())
                .build();
    }
}
