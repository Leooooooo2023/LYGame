package com.example.demo.pokemon.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 仓库实体类
 * 存储玩家存放在仓库中的精灵
 */
@Entity
@Table(name = "storage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageEntity {

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
     * 存放时间
     */
    @Column(name = "stored_time", nullable = false)
    private LocalDateTime storedTime;

    /**
     * 构造函数（带用户ID）
     */
    public StorageEntity(Long userId, PokemonEntity pokemon) {
        this.userId = userId;
        this.pokemon = pokemon;
        this.storedTime = LocalDateTime.now();
    }

    /**
     * 构造函数（兼容旧代码，使用默认用户ID 1）
     */
    public StorageEntity(PokemonEntity pokemon) {
        this.userId = 1L; // 默认用户ID
        this.pokemon = pokemon;
        this.storedTime = LocalDateTime.now();
    }
}
