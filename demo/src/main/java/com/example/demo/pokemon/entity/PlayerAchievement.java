package com.example.demo.pokemon.entity;

import com.example.demo.pokemon.enums.AchievementType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 玩家成就实体
 * 记录玩家已完成的成就
 */
@Entity
@Table(name = "player_achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAchievement {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 成就类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_type", nullable = false, length = 30)
    private AchievementType achievementType;

    /**
     * 完成时间
     */
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    /**
     * 是否已领取奖励
     */
    @Column(name = "reward_claimed", nullable = false)
    private boolean rewardClaimed = false;

    /**
     * 领取奖励时间
     */
    @Column(name = "reward_claimed_at")
    private LocalDateTime rewardClaimedAt;

    /**
     * 构造函数
     */
    public PlayerAchievement(Long userId, AchievementType achievementType) {
        this.userId = userId;
        this.achievementType = achievementType;
        this.completedAt = LocalDateTime.now();
        this.rewardClaimed = false;
    }
    
    /**
     * 兼容字符串playerId的构造函数
     * @deprecated 请使用 Long userId 的构造函数
     */
    @Deprecated
    public PlayerAchievement(String playerId, AchievementType achievementType) {
        this(1L, achievementType); // 默认用户ID为1
    }
}
