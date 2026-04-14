package com.example.demo.pokemon.service;

import com.example.demo.pokemon.entity.PlayerHealingItem;
import com.example.demo.pokemon.enums.HealingItemType;
import com.example.demo.pokemon.repository.PlayerHealingItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家恢复道具库存服务
 */
@Service
public class PlayerHealingItemService {

    private final PlayerHealingItemRepository healingItemRepository;

    private static final Long DEFAULT_USER_ID = 1L;

    @Autowired
    public PlayerHealingItemService(PlayerHealingItemRepository healingItemRepository) {
        this.healingItemRepository = healingItemRepository;
    }

    @Transactional
    public void initializeDefaultInventory() {
        initializeDefaultInventory(DEFAULT_USER_ID);
    }

    @Transactional
    public void initializeDefaultInventory(Long userId) {
        healingItemRepository.deleteByUserId(userId);

        for (HealingItemType type : HealingItemType.values()) {
            PlayerHealingItem inventory = new PlayerHealingItem();
            inventory.setUserId(userId);
            inventory.setItemType(type);
            inventory.setQuantity(0);
            healingItemRepository.save(inventory);
        }
    }

    @Transactional
    public void ensureDefaultInventory(Long userId) {
        if (!healingItemRepository.findByUserId(userId).isEmpty()) {
            return;
        }
        initializeDefaultInventory(userId);
    }

    public List<PlayerHealingItem> getPlayerInventory(Long userId) {
        return healingItemRepository.findByUserId(userId);
    }

    public int getItemCount(Long userId, HealingItemType itemType) {
        return healingItemRepository.findByUserIdAndItemType(userId, itemType)
                .map(PlayerHealingItem::getQuantity)
                .orElse(0);
    }

    @Transactional
    public boolean addItems(Long userId, HealingItemType itemType, int amount) {
        if (amount <= 0) {
            return false;
        }

        PlayerHealingItem inventory = healingItemRepository
                .findByUserIdAndItemType(userId, itemType)
                .orElseGet(() -> {
                    PlayerHealingItem newInv = new PlayerHealingItem();
                    newInv.setUserId(userId);
                    newInv.setItemType(itemType);
                    newInv.setQuantity(0);
                    return newInv;
                });

        inventory.setQuantity(inventory.getQuantity() + amount);
        healingItemRepository.save(inventory);
        return true;
    }

    @Transactional
    public boolean consumeItem(Long userId, HealingItemType itemType) {
        PlayerHealingItem inventory = healingItemRepository
                .findByUserIdAndItemType(userId, itemType)
                .orElse(null);

        if (inventory == null || inventory.getQuantity() <= 0) {
            return false;
        }

        inventory.setQuantity(inventory.getQuantity() - 1);
        healingItemRepository.save(inventory);
        return true;
    }

    public boolean hasEnoughItems(Long userId, HealingItemType itemType, int amount) {
        return getItemCount(userId, itemType) >= amount;
    }

    public Map<String, Integer> getPlayerInventoryMap(Long userId) {
        Map<String, Integer> result = new HashMap<>();
        List<PlayerHealingItem> inventories = getPlayerInventory(userId);

        for (PlayerHealingItem inv : inventories) {
            result.put(inv.getItemType().name(), inv.getQuantity());
        }

        for (HealingItemType type : HealingItemType.values()) {
            result.putIfAbsent(type.name(), 0);
        }

        return result;
    }

    public Map<String, Integer> getDefaultPlayerInventoryMap() {
        return getPlayerInventoryMap(DEFAULT_USER_ID);
    }

    public Long getDefaultUserId() {
        return DEFAULT_USER_ID;
    }
}
