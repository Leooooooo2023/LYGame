package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.PlayerHealingItem;
import com.example.demo.pokemon.enums.HealingItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 玩家恢复道具库存数据访问层
 */
@Repository
public interface PlayerHealingItemRepository extends JpaRepository<PlayerHealingItem, Long> {
    
    /**
     * 根据用户ID查询所有恢复道具
     */
    List<PlayerHealingItem> findByUserId(Long userId);
    
    /**
     * 根据用户ID和道具类型查询特定道具
     */
    Optional<PlayerHealingItem> findByUserIdAndItemType(Long userId, HealingItemType itemType);
    
    /**
     * 删除用户所有恢复道具（重置用）
     */
    void deleteByUserId(Long userId);
}
