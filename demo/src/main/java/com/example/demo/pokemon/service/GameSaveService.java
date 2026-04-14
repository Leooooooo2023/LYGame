package com.example.demo.pokemon.service;

import com.example.demo.pokemon.entity.BackpackEntity;
import com.example.demo.pokemon.entity.StorageEntity;
import com.example.demo.pokemon.entity.User;
import com.example.demo.pokemon.repository.BackpackRepository;
import com.example.demo.pokemon.repository.PlayerAchievementRepository;
import com.example.demo.pokemon.repository.PokedexRepository;
import com.example.demo.pokemon.repository.StorageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GameSaveService {

    private static final int INITIAL_GOLD = 200;
    private static final int STORAGE_BASE_SIZE = 30;

    private final BackpackRepository backpackRepository;
    private final StorageRepository storageRepository;
    private final PokedexRepository pokedexRepository;
    private final PlayerAchievementRepository achievementRepository;
    private final PlayerInventoryService playerInventoryService;
    private final PlayerHealingItemService playerHealingItemService;
    private final PokemonService pokemonService;
    private final AuthService authService;

    public GameSaveService(BackpackRepository backpackRepository,
                           StorageRepository storageRepository,
                           PokedexRepository pokedexRepository,
                           PlayerAchievementRepository achievementRepository,
                           PlayerInventoryService playerInventoryService,
                           PlayerHealingItemService playerHealingItemService,
                           PokemonService pokemonService,
                           AuthService authService) {
        this.backpackRepository = backpackRepository;
        this.storageRepository = storageRepository;
        this.pokedexRepository = pokedexRepository;
        this.achievementRepository = achievementRepository;
        this.playerInventoryService = playerInventoryService;
        this.playerHealingItemService = playerHealingItemService;
        this.pokemonService = pokemonService;
        this.authService = authService;
    }

    public Map<String, Object> getSaveStatus(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasSave", hasSaveData(userId));
        result.put("backpackCount", backpackRepository.countByUserId(userId));
        result.put("storageCount", storageRepository.countByUserId(userId));
        result.put("gold", authService.getUserById(userId).map(User::getGold).orElse(INITIAL_GOLD));
        return result;
    }

    public boolean hasSaveData(Long userId) {
        if (backpackRepository.countByUserId(userId) > 0 || storageRepository.countByUserId(userId) > 0) {
            return true;
        }

        if (!pokedexRepository.findByUserIdOrderByPokemonNameAsc(userId).isEmpty()) {
            return true;
        }

        return authService.getUserById(userId)
                .map(user -> user.getGold() != INITIAL_GOLD || user.getStorageCapacity() != STORAGE_BASE_SIZE)
                .orElse(false);
    }

    @Transactional
    public void resetForNewGame(Long userId) {
        List<BackpackEntity> backpackEntries = backpackRepository.findByUserIdOrderByCaughtTimeDesc(userId);
        List<StorageEntity> storageEntries = storageRepository.findByUserIdOrderByStoredTimeDesc(userId);
        Set<Long> ownedPokemonIds = new LinkedHashSet<>();

        backpackEntries.stream()
                .map(BackpackEntity::getPokemon)
                .filter(pokemon -> pokemon != null && pokemon.getId() != null)
                .forEach(pokemon -> ownedPokemonIds.add(pokemon.getId()));
        storageEntries.stream()
                .map(StorageEntity::getPokemon)
                .filter(pokemon -> pokemon != null && pokemon.getId() != null)
                .forEach(pokemon -> ownedPokemonIds.add(pokemon.getId()));

        backpackRepository.deleteAll(backpackEntries);
        storageRepository.deleteAll(storageEntries);

        ownedPokemonIds.forEach(pokemonService::deletePokemon);

        achievementRepository.deleteAll(achievementRepository.findByUserId(userId));
        pokedexRepository.deleteAll(pokedexRepository.findByUserIdOrderByPokemonNameAsc(userId));

        playerInventoryService.initializeDefaultInventory(userId);
        playerHealingItemService.initializeDefaultInventory(userId);
        authService.updateUserGold(userId, INITIAL_GOLD);
        authService.updateUserStorageCapacity(userId, STORAGE_BASE_SIZE);
    }
}
