package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.PlayerAchievement;
import com.example.demo.pokemon.enums.AchievementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 玩家成就数据访问层
 */
@Repository
public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {
    
    /**
     * 根据用户ID查询所有成就
     */
    List<PlayerAchievement> findByUserId(Long userId);
    
    /**
     * 根据用户ID和成就类型查询特定成就
     */
    Optional<PlayerAchievement> findByUserIdAndAchievementType(Long userId, AchievementType achievementType);
    
    /**
     * 查询用户未领取奖励的成就
     */
    List<PlayerAchievement> findByUserIdAndRewardClaimedFalse(Long userId);
    
    /**
     * 检查用户是否已完成某成就
     */
    boolean existsByUserIdAndAchievementType(Long userId, AchievementType achievementType);
}
