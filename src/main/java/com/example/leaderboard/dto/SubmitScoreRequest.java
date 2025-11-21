package com.example.leaderboard.dto;

public class SubmitScoreRequest {
    private Long userId;
    private Integer score;

    public SubmitScoreRequest() {}

    public SubmitScoreRequest(Long userId, Integer score) {
        this.userId = userId;
        this.score = score;
    }

    public Long getUserId() { return userId; }
    public Integer getScore() { return score; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setScore(Integer score) { this.score = score; }
}