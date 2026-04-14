package com.example.demo.pokemon.service;

import com.example.demo.pokemon.entity.PlayerAchievement;
import com.example.demo.pokemon.enums.AchievementType;
import com.example.demo.pokemon.enums.PokeBallType;
import com.example.demo.pokemon.repository.PlayerAchievementRepository;
import com.example.demo.pokemon.repository.PokedexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 成就系统服务
 */
@Service
public class AchievementService {

    private final PlayerAchievementRepository achievementRepository;
    private final PokedexRepository pokedexRepository;
    private final PlayerInventoryService inventoryService;

    @Autowired
    public AchievementService(PlayerAchievementRepository achievementRepository,
                              PokedexRepository pokedexRepository,
                              PlayerInventoryService inventoryService) {
        this.achievementRepository = achievementRepository;
        this.pokedexRepository = pokedexRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public List<PlayerAchievement> checkAchievements(Long userId, int currentGold) {
        List<PlayerAchievement> newAchievements = new ArrayList<>();
        int caughtCount = (int) pokedexRepository.countByUserIdAndCaughtTrue(userId);

        if (caughtCount >= 10) {
            createAchievementIfAbsent(userId, AchievementType.POKEDEX_10, newAchievements);
        }
        if (caughtCount >= 20) {
            createAchievementIfAbsent(userId, AchievementType.POKEDEX_20, newAchievements);
        }
        if (currentGold >= 2000) {
            createAchievementIfAbsent(userId, AchievementType.GOLD_2000, newAchievements);
        }
        if (currentGold >= 5000) {
            createAchievementIfAbsent(userId, AchievementType.GOLD_5000, newAchievements);
        }
        if (currentGold >= 10000) {
            createAchievementIfAbsent(userId, AchievementType.GOLD_10000, newAchievements);
        }

        return newAchievements;
    }

    private void createAchievementIfAbsent(Long userId,
                                           AchievementType type,
                                           List<PlayerAchievement> collector) {
        Optional<PlayerAchievement> existing = achievementRepository.findByUserIdAndAchievementType(userId, type);
        if (existing.isEmpty()) {
            PlayerAchievement achievement = new PlayerAchievement(userId, type);
            achievementRepository.save(achievement);
            collector.add(achievement);
        }
    }

    public List<PlayerAchievement> getPlayerAchievements(Long userId) {
        return achievementRepository.findByUserId(userId);
    }

    public List<PlayerAchievement> getUnclaimedAchievements(Long userId) {
        return achievementRepository.findByUserIdAndRewardClaimedFalse(userId);
    }

    @Transactional
    public Map<String, Object> claimAchievementReward(Long userId, Long achievementId, int currentGold) {
        Map<String, Object> result = new HashMap<>();

        Optional<PlayerAchievement> optAchievement = achievementRepository.findById(achievementId);
        if (optAchievement.isEmpty()) {
            result.put("success", false);
            result.put("message", "成就不存在");
            return result;
        }

        PlayerAchievement achievement = optAchievement.get();
        if (!Objects.equals(achievement.getUserId(), userId)) {
            result.put("success", false);
            result.put("message", "该成就不属于当前用户");
            return result;
        }
        if (achievement.isRewardClaimed()) {
            result.put("success", false);
            result.put("message", "奖励已领取");
            return result;
        }

        AchievementType type = achievement.getAchievementType();
        int goldReward = type.getGoldReward();
        int masterBallReward = type.getMasterBallReward();

        achievement.setRewardClaimed(true);
        achievement.setRewardClaimedAt(LocalDateTime.now());
        achievementRepository.save(achievement);

        if (masterBallReward > 0) {
            inventoryService.addBalls(userId, PokeBallType.MASTER, masterBallReward);
        }

        result.put("success", true);
        result.put("message", "奖励领取成功");
        result.put("goldReward", goldReward);
        result.put("masterBallReward", masterBallReward);
        result.put("achievementName", type.getDisplayName());
        return result;
    }

    public boolean isAchievementCompleted(Long userId, AchievementType type) {
        return achievementRepository.existsByUserIdAndAchievementType(userId, type);
    }

    public Map<String, Object> getAchievementStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        List<PlayerAchievement> achievements = achievementRepository.findByUserId(userId);

        int totalAchievements = AchievementType.values().length;
        int completedAchievements = achievements.size();
        int unclaimedRewards = (int) achievements.stream().filter(a -> !a.isRewardClaimed()).count();

        stats.put("total", totalAchievements);
        stats.put("completed", completedAchievements);
        stats.put("unclaimed", unclaimedRewards);
        stats.put("progress", totalAchievements > 0 ? (completedAchievements * 100.0 / totalAchievements) : 0.0);

        return stats;
    }
}
