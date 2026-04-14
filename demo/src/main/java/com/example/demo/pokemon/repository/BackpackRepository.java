package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.BackpackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 背包数据访问层
 * 提供背包相关的数据库操作
 */
@Repository
public interface BackpackRepository extends JpaRepository<BackpackEntity, Long> {

    /**
     * 查询所有背包中的精灵，按捕获时间倒序（兼容旧代码）
     */
    List<BackpackEntity> findAllByOrderByCaughtTimeDesc();

    /**
     * 查询指定用户的背包精灵，按捕获时间倒序
     */
    List<BackpackEntity> findByUserIdOrderByCaughtTimeDesc(Long userId);

    Optional<BackpackEntity> findByIdAndUserId(Long id, Long userId);

    /**
     * 统计背包中精灵数量（兼容旧代码）
     */
    long count();

    /**
     * 统计指定用户背包中精灵数量
     */
    long countByUserId(Long userId);
}
