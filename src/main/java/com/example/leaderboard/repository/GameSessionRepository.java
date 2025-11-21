package com.example.leaderboard.repository;

import com.example.leaderboard.model.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    // Basic repo; aggregation handled in service with native queries
}