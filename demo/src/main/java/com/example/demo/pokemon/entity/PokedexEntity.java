package com.example.demo.pokemon.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 精灵图鉴实体类
 * 记录玩家遇到/捕获过的精灵种类
 */
@Entity
@Table(name = "pokedex")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokedexEntity {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 精灵名称（唯一）
     */
    @Column(name = "pokemon_name", nullable = false)
    private String pokemonName;

    /**
     * 精灵属性
     */
    @Column(name = "pokemon_type", nullable = false)
    private String pokemonType;

    /**
     * 是否已捕获
     */
    @Column(name = "is_caught", nullable = false)
    private boolean caught = false;

    /**
     * 遇到次数
     */
    @Column(name = "encounter_count", nullable = false)
    private int encounterCount = 0;

    /**
     * 捕获次数
     */
    @Column(name = "catch_count", nullable = false)
    private int catchCount = 0;

    /**
     * 抽奖获得次数
     */
    @Column(name = "lottery_count", nullable = false)
    private int lotteryCount = 0;

    /**
     * 首次遇到时间
     */
    @Column(name = "first_encounter")
    private LocalDateTime firstEncounter;

    /**
     * 首次捕获时间
     */
    @Column(name = "first_catch")
    private LocalDateTime firstCatch;

    /**
     * 精灵描述/故事（由系统初始化时写入，丰富图鉴内容）
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 记录遇到
     */
    public void recordEncounter() {
        this.encounterCount++;
        if (this.firstEncounter == null) {
            this.firstEncounter = LocalDateTime.now();
        }
    }

    /**
     * 记录捕获
     */
    public void recordCatch() {
        this.caught = true;
        this.catchCount++;
        if (this.firstCatch == null) {
            this.firstCatch = LocalDateTime.now();
        }
    }

    /**
     * 记录抽奖获得
     */
    public void recordLottery() {
        this.caught = true;
        this.lotteryCount++;
        if (this.firstCatch == null) {
            this.firstCatch = LocalDateTime.now();
        }
    }
}
