package kg.metaacademy.controller;

import jakarta.validation.Valid;
import kg.metaacademy.dto.request.GroupCallRequest;
import kg.metaacademy.dto.response.GroupCallResponse;
import kg.metaacademy.security.SecurityUtils;
import kg.metaacademy.service.impl.GroupCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
public class GroupCallController {

    private final GroupCallService callService;
    private final SecurityUtils    security;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<GroupCallResponse>> byCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(callService.getByCourse(courseId));
    }

    @GetMapping("/course/{courseId}/upcoming")
    public ResponseEntity<List<GroupCallResponse>> upcoming(@PathVariable Long courseId) {
        return ResponseEntity.ok(callService.getUpcoming(courseId));
    }

    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GroupCallResponse> create(@PathVariable Long courseId,
                                                     @Valid @RequestBody GroupCallRequest req) {
        return ResponseEntity.ok(callService.create(security.getCurrentUserId(), courseId, req));
    }

    @PutMapping("/{callId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GroupCallResponse> update(@PathVariable Long callId,
                                                     @Valid @RequestBody GroupCallRequest req) {
        return ResponseEntity.ok(callService.update(callId, req));
    }

    @DeleteMapping("/{callId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long callId) {
        callService.delete(callId);
        return ResponseEntity.noContent().build();
    }
}
