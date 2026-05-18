package kg.metaacademy.controller;

import jakarta.validation.Valid;
import jakarta.validation.Valid;
import kg.metaacademy.dto.request.AssignmentRequest;
import kg.metaacademy.dto.request.GradeSubmissionRequest;
import kg.metaacademy.dto.request.SubmitAssignmentRequest;
import kg.metaacademy.dto.response.AssignmentResponse;
import kg.metaacademy.dto.response.SubmissionResponse;
import kg.metaacademy.security.SecurityUtils;
import kg.metaacademy.service.impl.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SecurityUtils     security;

    /** Преподаватель создаёт задание */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.ok(assignmentService.create(security.getCurrentUserId(), req));
    }

    /** Все задания курса (преподаватель) */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> byCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(assignmentService.getByCourse(courseId));
    }

    /** Задания для текущего студента */
    @GetMapping("/my/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AssignmentResponse>> myAssignments(@PathVariable Long courseId) {
        return ResponseEntity.ok(
                assignmentService.getForStudent(courseId, security.getCurrentUserId()));
    }

    /** Студент сдаёт задание */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmissionResponse> submit(@PathVariable Long id,
                                                      @Valid @RequestBody SubmitAssignmentRequest req) {
        return ResponseEntity.ok(assignmentService.submit(id, security.getCurrentUserId(), req));
    }

    /** Студент сдаёт задание файлом или zip */
    @PostMapping(value = "/{id}/submit-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmissionResponse> submitFile(@PathVariable Long id,
                                                         @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(assignmentService.submitFile(id, security.getCurrentUserId(), file));
    }

    /** Преподаватель оценивает работу */
    @PostMapping("/{assignmentId}/grade/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<SubmissionResponse> grade(@PathVariable Long assignmentId,
                                                     @PathVariable Long studentId,
                                                     @Valid @RequestBody GradeSubmissionRequest req) {
        return ResponseEntity.ok(assignmentService.grade(assignmentId, studentId, req));
    }

    /** Удалить задание */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
