package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.StorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 仓库数据访问层
 * 提供仓库相关的数据库操作
 */
@Repository
public interface StorageRepository extends JpaRepository<StorageEntity, Long> {

    /**
     * 查询所有仓库中的精灵，按存放时间倒序（兼容旧代码）
     */
    List<StorageEntity> findAllByOrderByStoredTimeDesc();

    /**
     * 查询指定用户的仓库精灵，按存放时间倒序
     */
    List<StorageEntity> findByUserIdOrderByStoredTimeDesc(Long userId);

    Optional<StorageEntity> findByIdAndUserId(Long id, Long userId);

    /**
     * 统计仓库中精灵数量（兼容旧代码）
     */
    long count();

    /**
     * 统计指定用户仓库中精灵数量
     */
    long countByUserId(Long userId);
}
