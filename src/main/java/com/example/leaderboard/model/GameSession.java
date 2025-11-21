package com.example.leaderboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_sessions")
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many sessions can belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int score;

    @Column(name = "game_mode", nullable = false)
    private String gameMode;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public GameSession() {}

    public GameSession(User user, int score, String gameMode) {
        this.user = user;
        this.score = score;
        this.gameMode = gameMode;
        this.timestamp = LocalDateTime.now();
    }

    // getters and setters

    public Long getId() { return id; }
    public User getUser() { return user; }
    public int getScore() { return score; }
    public String getGameMode() { return gameMode; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setScore(int score) { this.score = score; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}