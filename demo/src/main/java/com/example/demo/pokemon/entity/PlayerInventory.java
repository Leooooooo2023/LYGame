package com.example.demo.pokemon.entity;

import com.example.demo.pokemon.enums.PokeBallType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 玩家道具库存实体
 * 存储玩家拥有的各种精灵球数量
 */
@Entity
@Table(name = "player_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInventory {

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
     * 精灵球类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ball_type", nullable = false, length = 20)
    private PokeBallType ballType;

    /**
     * 数量
     */
    @Column(nullable = false)
    private int quantity = 0;

    /**
     * 构造函数
     */
    public PlayerInventory(Long userId, PokeBallType ballType, int quantity) {
        this.userId = userId;
        this.ballType = ballType;
        this.quantity = quantity;
    }
}
