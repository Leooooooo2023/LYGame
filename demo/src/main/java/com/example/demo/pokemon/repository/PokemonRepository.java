package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.PokemonEntity;
import com.example.demo.pokemon.enums.PokemonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 宝可梦数据访问层接口
 * 继承JpaRepository，提供基本的CRUD操作
 * 
 * 用法:
 * - pokemonRepository.findAll() - 查询所有宝可梦
 * - pokemonRepository.findById(id) - 根据ID查询
 * - pokemonRepository.save(pokemon) - 保存宝可梦
 * - pokemonRepository.deleteById(id) - 删除宝可梦
 */
@Repository
public interface PokemonRepository extends JpaRepository<PokemonEntity, Long> {

    /**
     * 根据名称查找宝可梦
     * @param name 宝可梦名称
     * @return 找到的宝可梦（Optional包装）
     */
    Optional<PokemonEntity> findByName(String name);

    /**
     * 根据属性类型查找宝可梦
     * @param type 属性类型
     * @return 该属性的所有宝可梦列表
     */
    List<PokemonEntity> findByType(PokemonType type);

    /**
     * 查找所有未濒死的宝可梦
     * @return 可战斗的宝可梦列表
     */
    List<PokemonEntity> findByFaintedFalse();

    /**
     * 根据等级范围查找宝可梦
     * @param minLevel 最小等级
     * @param maxLevel 最大等级
     * @return 该等级范围内的宝可梦列表
     */
    List<PokemonEntity> findByLevelBetween(int minLevel, int maxLevel);

    /**
     * 检查是否存在指定名称的宝可梦
     * @param name 宝可梦名称
     * @return 存在返回true
     */
    boolean existsByName(String name);
}
