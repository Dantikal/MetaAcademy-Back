package kg.metaacademy.controller;

import kg.metaacademy.dto.response.EnrollmentResponse;
import kg.metaacademy.security.SecurityUtils;
import kg.metaacademy.service.impl.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final SecurityUtils     security;

    @GetMapping("/my")
    public ResponseEntity<List<EnrollmentResponse>> my() {
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(security.getCurrentUserId()));
    }

    @PostMapping("/purchase/{courseId}")
    public ResponseEntity<EnrollmentResponse> purchase(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.purchase(security.getCurrentUserId(), courseId));
    }

    @PostMapping("/renew/{courseId}")
    public ResponseEntity<EnrollmentResponse> renew(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.renew(security.getCurrentUserId(), courseId));
    }
}
