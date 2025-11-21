package com.example.leaderboard.model;

import jakarta.persistence.*;

@Entity
@Table(name = "leaderboard")
public class LeaderboardEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // reference to user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_score", nullable = false)
    private Long totalScore;

    @Column(name = "rank")
    private Integer rank;

    public LeaderboardEntry() {}

    public LeaderboardEntry(User user, Long totalScore) {
        this.user = user;
        this.totalScore = totalScore;
    }

    // getters and setters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Long getTotalScore() { return totalScore; }
    public Integer getRank() { return rank; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setTotalScore(Long totalScore) { this.totalScore = totalScore; }
    public void setRank(Integer rank) { this.rank = rank; }
}