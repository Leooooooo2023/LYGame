package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.PokemonMoveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 宝可梦技能关联数据访问层接口
 */
@Repository
public interface PokemonMoveRepository extends JpaRepository<PokemonMoveEntity, Long> {

    /**
     * 根据宝可梦ID查找其所有技能
     * @param pokemonId 宝可梦ID
     * @return 技能关联列表
     */
    List<PokemonMoveEntity> findByPokemonId(Long pokemonId);

    /**
     * 根据技能ID查找所有拥有该技能的宝可梦
     * @param moveId 技能ID
     * @return 技能关联列表
     */
    List<PokemonMoveEntity> findByMoveId(Long moveId);

    /**
     * 删除宝可梦的所有技能关联
     * @param pokemonId 宝可梦ID
     */
    void deleteByPokemonId(Long pokemonId);
}
