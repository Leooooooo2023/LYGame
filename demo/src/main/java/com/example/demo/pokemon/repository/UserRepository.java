package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 本地存档数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 直接通过 JPQL 更新用户名，避免 Hibernate merge 操作导致的乐观锁异常
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.username = ?1 WHERE u.id = ?2")
    void updateUsernameById(String username, Long id);
}
