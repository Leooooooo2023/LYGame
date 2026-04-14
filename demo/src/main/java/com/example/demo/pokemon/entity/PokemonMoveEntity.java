package com.example.demo.pokemon.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 宝可梦技能关联实体类
 * 表示宝可梦和技能的多对多关系
 * 
 * 对应数据库表: pokemon_moves
 */
@Entity
@Table(name = "pokemon_moves")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonMoveEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的宝可梦
     */
    @ManyToOne
    @JoinColumn(name = "pokemon_id", nullable = false)
    private PokemonEntity pokemon;

    /**
     * 关联的技能
     */
    @ManyToOne
    @JoinColumn(name = "move_id", nullable = false)
    private MoveEntity move;

    /**
     * 构造函数
     */
    public PokemonMoveEntity(PokemonEntity pokemon, MoveEntity move) {
        this.pokemon = pokemon;
        this.move = move;
    }

    /**
     * 获取技能消耗的MP
     */
    public int getMpCost() {
        return move.getMpCost();
    }

    /**
     * 检查宝可梦是否有足够MP使用此技能
     */
    public boolean hasEnoughMp() {
        return pokemon.getCurrentMp() >= move.getMpCost();
    }
}
