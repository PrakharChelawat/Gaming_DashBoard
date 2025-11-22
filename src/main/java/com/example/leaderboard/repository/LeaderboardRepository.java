package com.example.leaderboard.repository;

import com.example.leaderboard.model.LeaderboardEntry;
import com.example.leaderboard.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {
    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO leaderboard (user_id, total_score)
    VALUES (:userId, :score)
    ON CONFLICT (user_id) DO UPDATE
    SET total_score = leaderboard.total_score + :score
""", nativeQuery = true)
    void incrementTotalScore(@Param("userId") Long userId, @Param("score") int score);

    @Modifying
    @Transactional
    @Query(value = """
    WITH ranked AS (
        SELECT user_id, DENSE_RANK() OVER (ORDER BY total_score DESC) AS new_rank
        FROM leaderboard
    )
    UPDATE leaderboard l
    SET rank = r.new_rank
    FROM ranked r
    WHERE l.user_id = r.user_id;
""", nativeQuery = true)
    void updateAllRanks();

    Optional<LeaderboardEntry> findByUser(User user);

    List<LeaderboardEntry> findAllByOrderByTotalScoreDesc(Pageable pageable);
}