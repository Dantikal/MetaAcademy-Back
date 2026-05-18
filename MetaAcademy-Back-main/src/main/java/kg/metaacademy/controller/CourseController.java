package kg.metaacademy.controller;

import jakarta.validation.Valid;
import kg.metaacademy.dto.request.CourseRequest;
import kg.metaacademy.dto.response.CourseResponse;
import kg.metaacademy.service.impl.CourseService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getActive() {
        return ResponseEntity.ok(courseService.getAllActive());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourseResponse>> getAll() {
        return ResponseEntity.ok(courseService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getById(id));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<CourseResponse>> getByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(courseService.getByTeacher(teacherId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest req) {
        return ResponseEntity.ok(courseService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<CourseResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody CourseRequest req) {
        return ResponseEntity.ok(courseService.update(id, req));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id) {
        courseService.toggleActive(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Создать 4 английских курса (Beginner / Elementary / Pre-Intermediate / Intermediate).
     * POST /api/courses/setup-english
     * Тело: [ { level: "Beginner", teacherId: 5 }, ... ]
     * Пропускает уже существующие курсы.
     */
    @PostMapping("/setup-english")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourseResponse>> setupEnglish(
            @RequestBody java.util.List<java.util.Map<String,Object>> levels) {
        return ResponseEntity.ok(courseService.setupEnglishCourses(levels));
    }

    /**
     * Обновить преподавателя конкретного английского курса.
     * PATCH /api/courses/{id}/assign-teacher?teacherId=X
     */
    @PatchMapping("/{id}/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> assignTeacher(
            @PathVariable Long id,
            @RequestParam Long teacherId) {
        return ResponseEntity.ok(courseService.assignTeacher(id, teacherId));
    }
}
