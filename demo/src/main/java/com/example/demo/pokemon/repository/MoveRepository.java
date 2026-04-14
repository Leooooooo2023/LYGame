package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.MoveEntity;
import com.example.demo.pokemon.enums.PokemonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 技能数据访问层接口
 * 继承JpaRepository，提供基本的CRUD操作
 */
@Repository
public interface MoveRepository extends JpaRepository<MoveEntity, Long> {

    /**
     * 根据名称查找技能
     * @param name 技能名称
     * @return 找到的技能
     */
    Optional<MoveEntity> findByName(String name);

    /**
     * 根据属性类型查找技能
     * @param type 属性类型
     * @return 该属性的所有技能列表
     */
    List<MoveEntity> findByType(PokemonType type);

    /**
     * 根据威力范围查找技能
     * @param minPower 最小威力
     * @param maxPower 最大威力
     * @return 该威力范围内的技能列表
     */
    List<MoveEntity> findByPowerBetween(int minPower, int maxPower);

    /**
     * 查找所有高威力技能（威力>=90）
     * @return 高威力技能列表
     */
    List<MoveEntity> findByPowerGreaterThanEqual(int power);
}
