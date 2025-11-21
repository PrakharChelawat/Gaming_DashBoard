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
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class LeaderboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private EntityManager entityManager;

    @Transactional
    public PlayerRankDto submitScore(Long userId, int score) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        GameSession session = new GameSession(user, score, "default");
        gameSessionRepository.save(session);

        // Recalculate totals from game_sessions
        List<Object[]> totals = entityManager.createNativeQuery(
                "SELECT user_id, SUM(score) as total FROM game_sessions GROUP BY user_id")
                .getResultList();

        Map<Long, Integer> totalByUser = new HashMap<>();
        for (Object row : totals) {
            Object[] arr = (Object[]) row;
            Number uid = (Number) arr[0];
            Number tot = (Number) arr[1];
            totalByUser.put(uid.longValue(), tot.intValue());
        }

        // Update/create entries in leaderboard table
        for (Map.Entry<Long, Integer> e : totalByUser.entrySet()) {
            Long uid = e.getKey();
            Integer total = e.getValue();
            Optional<User> uopt = userRepository.findById(uid);
            if (uopt.isEmpty()) continue;
            User u = uopt.get();
            Optional<LeaderboardEntry> opt = leaderboardRepository.findByUser(u);
            LeaderboardEntry entry;
            if (opt.isPresent()) {
                entry = opt.get();
                entry.setTotalScore(Long.valueOf(total));
            } else {
                entry = new LeaderboardEntry(u, Long.valueOf(total));
            }
            leaderboardRepository.save(entry);
        }

        // Optionally remove leaderboard entries for users with no sessions
        List<LeaderboardEntry> allEntries = leaderboardRepository.findAll();
        for (LeaderboardEntry e : allEntries) {
            if (!totalByUser.containsKey(e.getUser().getId())) {
                leaderboardRepository.delete(e);
            }
        }

        // Recompute ranks (dense ranking — equal scores same rank)
        List<LeaderboardEntry> ordered = leaderboardRepository.findAllByOrderByTotalScoreDesc();
        Integer currentRank = 0;
        Long prevScore = null;
        Integer distinctRankCounter = 0;
        for (LeaderboardEntry e : ordered) {
            if (prevScore == null || e.getTotalScore() < prevScore) {
                distinctRankCounter++;
                currentRank = distinctRankCounter;
            }
            e.setRank(currentRank);
            prevScore = e.getTotalScore();
            leaderboardRepository.save(e);
        }

        // Return the requesting player's rank and total
        LeaderboardEntry myEntry = leaderboardRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Leaderboard entry missing after update"));
        return new PlayerRankDto(user.getId(), user.getUsername(), myEntry.getTotalScore(), myEntry.getRank());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getTop(int limit) {
        List<LeaderboardEntry> list = leaderboardRepository.findAllByOrderByTotalScoreDesc(PageRequest.of(0, limit));
        List<LeaderboardEntryDto> res = new ArrayList<>();
        for (LeaderboardEntry e : list) {
            res.add(new LeaderboardEntryDto(e.getUser().getId(), e.getUser().getUsername(), e.getTotalScore(), e.getRank()));
        }
        return res;
    }

    @Transactional(readOnly = true)
    public PlayerRankDto getPlayerRank(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Optional<LeaderboardEntry> opt = leaderboardRepository.findByUser(user);
        if (opt.isEmpty()) {
            // If player has no score, return rank = null or indicate not ranked
            return new PlayerRankDto(user.getId(), user.getUsername(), Long.valueOf(0), null);
        }
        LeaderboardEntry e = opt.get();
        return new PlayerRankDto(user.getId(), user.getUsername(), e.getTotalScore(), e.getRank());
    }
}