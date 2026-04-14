package com.example.demo.pokemon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单机版本地存档实体
 * 保存本地玩家的基础持久化数据。
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 存档名称
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 金币数量
     */
    @Column(name = "gold", nullable = false)
    private int gold = 0;

    /**
     * 仓库容量
     */
    @Column(name = "storage_capacity", nullable = false)
    private int storageCapacity = 30;

    /**
     * 存档创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
