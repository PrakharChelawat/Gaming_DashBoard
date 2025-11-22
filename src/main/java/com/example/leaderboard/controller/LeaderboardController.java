package com.example.leaderboard.controller;

import com.example.leaderboard.dto.SubmitScoreRequest;
import com.example.leaderboard.dto.LeaderboardEntryDto;
import com.example.leaderboard.dto.PlayerRankDto;
import com.example.leaderboard.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitScore(@RequestBody SubmitScoreRequest req) {
        if (req.getUserId() == null || req.getScore() == null) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Object> response = leaderboardService.submitScore(req.getUserId(), req.getScore());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top")
    public ResponseEntity<List<LeaderboardEntryDto>> getTop() {
        List<LeaderboardEntryDto> top = leaderboardService.getTop(10);
        return ResponseEntity.ok(top);
    }

    @GetMapping("/rank/{userId}")
    public ResponseEntity<PlayerRankDto> getRank(@PathVariable("userId") Long userId) {
        PlayerRankDto dto = leaderboardService.getPlayerRank(userId);
        return ResponseEntity.ok(dto);
    }
}