package com.example.leaderboard.service;

import com.example.leaderboard.dto.LeaderboardEntryDto;
import com.example.leaderboard.dto.PlayerRankDto;
import com.example.leaderboard.model.GameSession;
import com.example.leaderboard.model.LeaderboardEntry;
import com.example.leaderboard.model.User;
import com.example.leaderboard.repository.GameSessionRepository;
import com.example.leaderboard.repository.LeaderboardRepository;
import com.example.leaderboard.repository.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Slf4j
@Service
public class LeaderboardService {

    @Autowired private UserRepository userRepository;
    @Autowired private GameSessionRepository gameSessionRepository;
    @Autowired private LeaderboardRepository leaderboardRepository;
    @Autowired private EntityManager entityManager;

    @Transactional
    @CacheEvict(value = "topLeaderboard", allEntries = true)
    public Map<String, Object> submitScore(Long userId, int score) {
        log.info("submitScore() called for userId={}, score={}", userId, score);

        // 1️⃣ Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2️⃣ Save new game session
        GameSession session = new GameSession(user, score, "default");
        gameSessionRepository.save(session);
        log.debug("Saved new game session for userId={} with score={}", userId, score);

        // 3️⃣ Increment or insert leaderboard total
        leaderboardRepository.incrementTotalScore(userId, score);
        log.debug("Incremented leaderboard total for userId={} by {}", userId, score);

        // 4️⃣ Recompute ranks for all users (single DB query)
        leaderboardRepository.updateAllRanks();
        log.debug("Leaderboard ranks recomputed");

        log.info("Score submitted successfully for userId={}", userId);

        // 5️⃣ Return simple success response
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("message", "Total score updated successfully");
        return response;
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "topLeaderboard", key = "#limit")
    public List<LeaderboardEntryDto> getTop(int limit) {
        log.info("getTop() called, limit={}", limit);

        List<LeaderboardEntry> list =
                leaderboardRepository.findAllByOrderByTotalScoreDesc(PageRequest.of(0, limit));

        log.debug("Fetched {} leaderboard entries", list.size());

        List<LeaderboardEntryDto> res = new ArrayList<>();
        for (LeaderboardEntry e : list) {
            res.add(new LeaderboardEntryDto(
                    e.getUser().getId(),
                    e.getUser().getUsername(),
                    e.getTotalScore(),
                    e.getRank()
            ));
        }
        return res;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userRank", key = "#userId")
    public PlayerRankDto getPlayerRank(Long userId) {

        log.info("getPlayerRank() called for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Optional<LeaderboardEntry> opt = leaderboardRepository.findByUser(user);

        if (opt.isEmpty()) {
            log.info("User {} is not ranked yet", userId);
            return new PlayerRankDto(userId, user.getUsername(), 0L, null);
        }

        LeaderboardEntry e = opt.get();

        log.info("User {} rank={}, score={}", userId, e.getRank(), e.getTotalScore());

        return new PlayerRankDto(
                user.getId(),
                user.getUsername(),
                e.getTotalScore(),
                e.getRank()
        );
    }

    @Autowired
    private CacheManager cacheManager;

    public void printTopLeaderboardCache() {
        Cache cache = cacheManager.getCache("topLeaderboard");
        if (cache != null) {
            Object cachedValue = cache.get(10, Object.class); // example for top 10
            System.out.println("Cached Top 10 leaderboard: " + cachedValue);
        }
    }
}