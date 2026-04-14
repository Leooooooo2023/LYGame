package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.PlayerInventory;
import com.example.demo.pokemon.enums.PokeBallType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 玩家道具库存数据访问层
 */
@Repository
public interface PlayerInventoryRepository extends JpaRepository<PlayerInventory, Long> {

    /**
     * 根据用户ID查询所有道具
     */
    List<PlayerInventory> findByUserId(Long userId);

    /**
     * 根据用户ID和精灵球类型查询
     */
    Optional<PlayerInventory> findByUserIdAndBallType(Long userId, PokeBallType ballType);

    /**
     * 删除用户所有道具（重置用）
     */
    void deleteByUserId(Long userId);
}
