package kg.metaacademy.controller;

import jakarta.validation.Valid;
import kg.metaacademy.dto.request.UpdateProfileRequest;
import kg.metaacademy.dto.response.UserResponse;
import kg.metaacademy.exception.BadRequestException;
import kg.metaacademy.security.SecurityUtils;
import kg.metaacademy.dto.response.AdminStudentResponse;
import kg.metaacademy.service.impl.EnrollmentService;
import kg.metaacademy.service.impl.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService       userService;
    private final EnrollmentService  enrollmentService;
    private final SecurityUtils      security;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Long userId = security.getCurrentUserId();
        return ResponseEntity.ok(userService.getById(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(security.getCurrentUserId(), req));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(userService.uploadAvatar(security.getCurrentUserId(), file));
    }

    @PostMapping("/me/english-level")
    public ResponseEntity<Void> setEnglishLevel(@RequestParam String level) {
        userService.setEnglishLevel(security.getCurrentUserId(), level);
        return ResponseEntity.ok().build();
    }

    // ADMIN endpoints
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> all() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(userService.search(q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    // ИСПРАВЛЕН: нельзя заблокировать самого себя
    @PatchMapping("/{id}/toggle-block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleBlock(@PathVariable Long id) {
        if (id.equals(security.getCurrentUserId())) {
            throw new BadRequestException("Нельзя заблокировать самого себя");
        }
        userService.toggleBlock(id);
        return ResponseEntity.noContent().build();
    }

    // ИСПРАВЛЕН: нельзя удалить самого себя
    @GetMapping("/admin/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminStudentResponse>> adminStudents() {
        return ResponseEntity.ok(enrollmentService.getAllStudentsForAdmin());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id.equals(security.getCurrentUserId())) {
            throw new BadRequestException("Нельзя удалить самого себя");
        }
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
