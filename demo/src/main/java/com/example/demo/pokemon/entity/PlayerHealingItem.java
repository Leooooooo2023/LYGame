package com.example.demo.pokemon.entity;

import com.example.demo.pokemon.enums.HealingItemType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 玩家恢复道具库存实体
 * 存储玩家拥有的各种恢复类道具数量
 */
@Entity
@Table(name = "player_healing_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerHealingItem {

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
     * 恢复道具类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private HealingItemType itemType;

    /**
     * 数量
     */
    @Column(nullable = false)
    private int quantity = 0;

    /**
     * 构造函数
     */
    public PlayerHealingItem(Long userId, HealingItemType itemType, int quantity) {
        this.userId = userId;
        this.itemType = itemType;
        this.quantity = quantity;
    }
}
