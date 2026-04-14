package com.example.demo.pokemon.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 背包实体类
 * 存储玩家拥有的精灵
 */
@Entity
@Table(name = "backpack")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackpackEntity {

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
     * 关联的精灵
     */
    @ManyToOne
    @JoinColumn(name = "pokemon_id", nullable = false)
    private PokemonEntity pokemon;

    /**
     * 捕获时间
     */
    @Column(name = "caught_time", nullable = false)
    private LocalDateTime caughtTime;

    /**
     * 是否已被使用（出战过）
     */
    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    /**
     * 构造函数（带用户ID）
     */
    public BackpackEntity(Long userId, PokemonEntity pokemon) {
        this.userId = userId;
        this.pokemon = pokemon;
        this.caughtTime = LocalDateTime.now();
        this.used = false;
    }

    /**
     * 构造函数（兼容旧代码，使用默认用户ID 1）
     */
    public BackpackEntity(PokemonEntity pokemon) {
        this.userId = 1L; // 默认用户ID
        this.pokemon = pokemon;
        this.caughtTime = LocalDateTime.now();
        this.used = false;
    }
}
