package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.PokedexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 精灵图鉴数据访问层
 * 提供图鉴相关的数据库操作
 */
@Repository
public interface PokedexRepository extends JpaRepository<PokedexEntity, Long> {

    /**
     * 根据精灵名称查找图鉴记录
     */
    Optional<PokedexEntity> findByUserIdAndPokemonName(Long userId, String pokemonName);

    List<PokedexEntity> findByUserIdOrderByPokemonNameAsc(Long userId);

    /**
     * 统计已捕获的精灵种类数
     */
    long countByUserIdAndCaughtTrue(Long userId);

    /**
     * 统计已遇到的精灵种类数
     */
    long countByUserIdAndEncounterCountGreaterThan(Long userId, int count);
}
