package kg.metaacademy.controller;

import jakarta.validation.Valid;
import kg.metaacademy.dto.request.QuizSubmitRequest;
import kg.metaacademy.dto.response.LessonResponse;
import kg.metaacademy.dto.response.QuizResultResponse;
import kg.metaacademy.security.SecurityUtils;
import kg.metaacademy.service.impl.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
    private final SecurityUtils   security;

    /** Уроки курса с прогрессом студента */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LessonResponse>> getCourseProgress(@PathVariable Long courseId) {
        return ResponseEntity.ok(
                progressService.getCourseWithProgress(security.getCurrentUserId(), courseId));
    }

    /** Один урок с прогрессом */
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<LessonResponse> getLessonProgress(@PathVariable Long lessonId) {
        return ResponseEntity.ok(
                progressService.getLessonWithProgress(security.getCurrentUserId(), lessonId));
    }

    /** Отметить видео просмотренным */
    @PostMapping("/lesson/{lessonId}/video")
    public ResponseEntity<Void> markVideo(@PathVariable Long lessonId) {
        progressService.markVideoWatched(security.getCurrentUserId(), lessonId);
        return ResponseEntity.ok().build();
    }

    /** Сдать тест */
    @PostMapping("/quiz")
    public ResponseEntity<QuizResultResponse> submitQuiz(@Valid @RequestBody QuizSubmitRequest req) {
        return ResponseEntity.ok(
                progressService.submitQuiz(security.getCurrentUserId(), req));
    }

    /** Завершить практику (режим "text" — студент написал код в редакторе) */
    @PostMapping("/lesson/{lessonId}/practice")
    public ResponseEntity<Void> completePractice(@PathVariable Long lessonId) {
        progressService.completePractice(security.getCurrentUserId(), lessonId);
        return ResponseEntity.ok().build();
    }

    /** Сдать практику файлом (режим "file" — студент загрузил .zip или исходник) */
    @PostMapping(value = "/lesson/{lessonId}/practice-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> submitPracticeFile(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                progressService.submitPracticeFile(security.getCurrentUserId(), lessonId, file));
    }
}
