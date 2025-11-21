package com.example.leaderboard.dto;

public class LeaderboardEntryDto {
    private Long userId;
    private String username;
    private Long totalScore;
    private Integer rank;

    public LeaderboardEntryDto() {}

    public LeaderboardEntryDto(Long userId, String username, Long totalScore, Integer rank) {
        this.userId = userId;
        this.username = username;
        this.totalScore = totalScore;
        this.rank = rank;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public Long getTotalScore() { return totalScore; }
    public Integer getRank() { return rank; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setTotalScore(Long totalScore) { this.totalScore = totalScore; }
    public void setRank(Integer rank) { this.rank = rank; }
}