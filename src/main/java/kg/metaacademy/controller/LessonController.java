package kg.metaacademy.controller;

import jakarta.validation.Valid;
import kg.metaacademy.dto.request.LessonRequest;
import kg.metaacademy.dto.request.SaveQuizRequest;
import kg.metaacademy.dto.response.LessonResponse;
import kg.metaacademy.service.impl.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LessonResponse>> byCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonService.getByCourse(courseId));
    }

    /** Уроки английского курса по уровню студента */
    @GetMapping("/course/{courseId}/level/{engLevel}")
    public ResponseEntity<List<LessonResponse>> byLevel(
            @PathVariable Long courseId,
            @PathVariable String engLevel) {
        return ResponseEntity.ok(lessonService.getByEngLevel(courseId, engLevel));
    }

    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> create(@PathVariable Long courseId,
                                                  @Valid @RequestBody LessonRequest req) {
        return ResponseEntity.ok(lessonService.create(courseId, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody LessonRequest req) {
        return ResponseEntity.ok(lessonService.update(id, req));
    }

    @PostMapping("/{id}/video")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> uploadVideo(@PathVariable Long id,
                                                       @RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(lessonService.uploadVideo(id, file));
    }

    @PostMapping("/{id}/quiz")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> saveQuiz(@PathVariable Long id,
                                                    @Valid @RequestBody SaveQuizRequest req) {
        return ResponseEntity.ok(lessonService.saveQuiz(id, req.getQuestions()));
    }

    /** Старый эндпоинт — только текст задания (обратная совместимость) */
    @PostMapping("/{id}/practice")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> savePractice(@PathVariable Long id,
                                                        @RequestBody String task) {
        return ResponseEntity.ok(lessonService.savePractice(id, task));
    }

    /** Новый эндпоинт — текст задания + режим ("text" или "file") */
    @PostMapping("/{id}/practice-mode")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> savePracticeWithMode(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String task = body.getOrDefault("task", "");
        String mode = body.getOrDefault("mode", "text");
        return ResponseEntity.ok(lessonService.savePracticeWithMode(id, task, mode));
    }

    /** Сохранить настройки AI-практики для английского урока */
    @PostMapping("/{id}/ai-practice")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> saveAiPractice(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String aiQuestions = (String) body.get("aiQuestions");
        Integer aiQuestionCount = body.get("aiQuestionCount") instanceof Number n ? n.intValue() : null;
        return ResponseEntity.ok(lessonService.saveAiPractice(id, aiQuestions, aiQuestionCount));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
