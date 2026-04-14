package com.example.demo.pokemon.service;

import com.example.demo.pokemon.entity.PlayerInventory;
import com.example.demo.pokemon.enums.PokeBallType;
import com.example.demo.pokemon.repository.PlayerInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家道具库存服务
 * 管理玩家拥有的精灵球等道具
 */
@Service
public class PlayerInventoryService {

    private final PlayerInventoryRepository inventoryRepository;
    
    // 初始初级精灵球数量
    private static final int INITIAL_BASIC_BALLS = 10;

    @Autowired
    public PlayerInventoryService(PlayerInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    // 默认用户ID（兼容旧代码）
    private static final Long DEFAULT_USER_ID = 1L;

    /**
     * 初始化默认道具库存（兼容旧代码）
     */
    @Transactional
    public void initializeDefaultInventory() {
        initializeDefaultInventory(DEFAULT_USER_ID);
    }

    @Transactional
    public void ensureDefaultInventory(Long userId) {
        if (!inventoryRepository.findByUserId(userId).isEmpty()) {
            return;
        }
        initializeDefaultInventory(userId);
    }

    /**
     * 初始化用户默认道具库存
     * 新用户默认获得10个初级精灵球
     */
    @Transactional
    public void initializeDefaultInventory(Long userId) {
        // 清空现有库存（重新初始化）
        inventoryRepository.deleteByUserId(userId);
        
        // 初始化四种精灵球，数量为0
        for (PokeBallType type : PokeBallType.values()) {
            PlayerInventory inventory = new PlayerInventory();
            inventory.setUserId(userId);
            inventory.setBallType(type);
            inventory.setQuantity(0);
            inventoryRepository.save(inventory);
        }
        
        // 给予10个初级精灵球
        addBalls(userId, PokeBallType.BASIC, INITIAL_BASIC_BALLS);
    }

    /**
     * 获取用户所有道具
     */
    public List<PlayerInventory> getPlayerInventory(Long userId) {
        return inventoryRepository.findByUserId(userId);
    }

    /**
     * 获取用户特定类型精灵球数量
     */
    public int getBallCount(Long userId, PokeBallType ballType) {
        return inventoryRepository.findByUserIdAndBallType(userId, ballType)
                .map(PlayerInventory::getQuantity)
                .orElse(0);
    }

    /**
     * 增加精灵球数量
     */
    @Transactional
    public boolean addBalls(Long userId, PokeBallType ballType, int amount) {
        if (amount <= 0) return false;
        
        PlayerInventory inventory = inventoryRepository
                .findByUserIdAndBallType(userId, ballType)
                .orElseGet(() -> {
                    PlayerInventory newInv = new PlayerInventory();
                    newInv.setUserId(userId);
                    newInv.setBallType(ballType);
                    newInv.setQuantity(0);
                    return newInv;
                });
        
        inventory.setQuantity(inventory.getQuantity() + amount);
        inventoryRepository.save(inventory);
        return true;
    }

    /**
     * 消耗精灵球
     * @return 是否成功消耗
     */
    @Transactional
    public boolean consumeBall(Long userId, PokeBallType ballType) {
        PlayerInventory inventory = inventoryRepository
                .findByUserIdAndBallType(userId, ballType)
                .orElse(null);
        
        if (inventory == null || inventory.getQuantity() <= 0) {
            return false;
        }
        
        inventory.setQuantity(inventory.getQuantity() - 1);
        inventoryRepository.save(inventory);
        return true;
    }

    /**
     * 检查是否有足够的精灵球
     */
    public boolean hasEnoughBalls(Long userId, PokeBallType ballType, int amount) {
        return getBallCount(userId, ballType) >= amount;
    }

    /**
     * 获取用户的道具库存（Map形式）
     */
    public Map<String, Integer> getPlayerInventoryMap(Long userId) {
        Map<String, Integer> result = new HashMap<>();
        List<PlayerInventory> inventories = getPlayerInventory(userId);
        
        for (PlayerInventory inv : inventories) {
            result.put(inv.getBallType().name(), inv.getQuantity());
        }
        
        // 确保所有类型都有值
        for (PokeBallType type : PokeBallType.values()) {
            result.putIfAbsent(type.name(), 0);
        }
        
        return result;
    }

    /**
     * 获取默认玩家的道具库存（Map形式）- 兼容旧代码
     */
    public Map<String, Integer> getDefaultPlayerInventoryMap() {
        return getPlayerInventoryMap(DEFAULT_USER_ID);
    }

    /**
     * 获取默认玩家ID - 兼容旧代码
     */
    public String getDefaultPlayerId() {
        return DEFAULT_USER_ID.toString();
    }
}
