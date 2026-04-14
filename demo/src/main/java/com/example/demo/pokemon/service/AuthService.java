package com.example.demo.pokemon.service;

import com.example.demo.pokemon.entity.User;
import com.example.demo.pokemon.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 本地单机存档服务
 * 仍然复用 users 表保存金币和仓库容量，但不再提供登录、注册、管理员能力。
 */
@Service
public class AuthService {

    public static final Long LOCAL_USER_ID = 1L; 
    public static final String LOCAL_USERNAME = "本地玩家";                                      
    private final UserRepository userRepository; 
    private final PlayerInventoryService playerInventoryService;
    private final PlayerHealingItemService playerHealingItemService;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public AuthService(UserRepository userRepository,
                       PlayerInventoryService playerInventoryService,
                       PlayerHealingItemService playerHealingItemService,
                       JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;    
        this.playerInventoryService = playerInventoryService;
        this.playerHealingItemService = playerHealingItemService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // 使用 JdbcTemplate 直接插入，避免 Hibernate merge 导致的并发问题
    public User ensureLocalUser() {
        Optional<User> existing = userRepository.findById(LOCAL_USER_ID);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 使用 insert 而不是 save，避免 Hibernate 的 merge 操作
        String sql = "INSERT INTO users (id, username, gold, storage_capacity, created_at) VALUES (?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, LOCAL_USER_ID, LOCAL_USERNAME, 200, 30);
        
        return userRepository.findById(LOCAL_USER_ID).get();
    }

    public Optional<User> getUserById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id);
    }

    @Transactional
    public boolean updateUserGold(Long userId, int newGold) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setGold(newGold);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean updateUserStorageCapacity(Long userId, int storageCapacity) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStorageCapacity(storageCapacity);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}

