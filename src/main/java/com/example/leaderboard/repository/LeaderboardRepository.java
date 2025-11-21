package com.example.leaderboard.repository;

import com.example.leaderboard.model.LeaderboardEntry;
import com.example.leaderboard.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {
    Optional<LeaderboardEntry> findByUser(User user);
    List<LeaderboardEntry> findAllByOrderByTotalScoreDesc(Pageable pageable);
    List<LeaderboardEntry> findAllByOrderByTotalScoreDesc();
}