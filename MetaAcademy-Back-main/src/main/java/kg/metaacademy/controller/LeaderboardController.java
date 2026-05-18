package kg.metaacademy.controller;

import kg.metaacademy.dto.response.LeaderboardEntry;
import kg.metaacademy.security.SecurityUtils;
import kg.metaacademy.service.impl.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final SecurityUtils      security;

    @GetMapping("/global")
    public ResponseEntity<List<LeaderboardEntry>> global() {
        return ResponseEntity.ok(leaderboardService.getGlobal(security.getCurrentUserId()));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LeaderboardEntry>> byCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(leaderboardService.getByCourse(courseId, security.getCurrentUserId()));
    }
}
