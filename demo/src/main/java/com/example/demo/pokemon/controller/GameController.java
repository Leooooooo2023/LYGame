package com.example.demo.pokemon.controller;

import com.example.demo.pokemon.entity.BackpackEntity;
import com.example.demo.pokemon.entity.MoveEntity;
import com.example.demo.pokemon.enums.MoveEffectType;
import com.example.demo.pokemon.entity.PlayerAchievement;
import com.example.demo.pokemon.entity.PlayerEgg;
import com.example.demo.pokemon.entity.PokedexEntity;
import com.example.demo.pokemon.entity.PokemonEntity;
import com.example.demo.pokemon.entity.PokemonMoveEntity;
import com.example.demo.pokemon.entity.StorageEntity;
import com.example.demo.pokemon.enums.AchievementType;
import com.example.demo.pokemon.enums.EggStatus;
import com.example.demo.pokemon.enums.HealingItemType;
import com.example.demo.pokemon.enums.PokeBallType;
import com.example.demo.pokemon.enums.PokemonGender;
import com.example.demo.pokemon.enums.PokemonType;
import com.example.demo.pokemon.repository.BackpackRepository;
import com.example.demo.pokemon.repository.PokedexRepository;
import com.example.demo.pokemon.repository.PlayerEggRepository;
import com.example.demo.pokemon.repository.PokemonRepository;
import com.example.demo.pokemon.repository.StorageRepository;
import com.example.demo.pokemon.service.AchievementService;
import com.example.demo.pokemon.service.AuthService;
import com.example.demo.pokemon.service.GameSaveService;
import com.example.demo.pokemon.service.PlayerHealingItemService;
import com.example.demo.pokemon.service.PlayerInventoryService;
import com.example.demo.pokemon.service.PokemonService;
import com.example.demo.pokemon.service.TypeEffectivenessService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
public class GameController {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final int BACKPACK_MAX_SIZE = 6;
    private static final int STORAGE_BASE_SIZE = 30;
    private static final int STORAGE_EXPAND_COST = 1000;
    private static final int STORAGE_EXPAND_AMOUNT = 10;
    private static final int STORAGE_MAX_LIMIT = 100;
    private static final int INITIAL_GOLD = 200;
    private static final int WILD_BATTLE_REWARD = 100;
    private static final int TRAINER_BATTLE_REWARD = 1000;
    private static final int DUNGEON_CLEAR_REWARD = 1000;
    private static final int WILD_EXP_BASE = 30;
    private static final int TRAINER_EXP_BASE = 45;
    private static final int DUNGEON_EXP_BASE = 60;
    private static final int TRAINER_CLEAR_EXP_BONUS = 90;
    private static final int DUNGEON_CLEAR_EXP_BONUS = 150;
    private static final int CATCH_SUCCESS_REWARD = 100;
    private static final int HOSPITAL_HEAL_COST = 50;
    private static final int LOTTERY_COST = 100;
    private static final int TRAINER_BATTLE_POKEMON_COUNT = 6;
    private static final int BREEDING_COST = 500;
    private static final int MAX_ACTIVE_EGGS = 3;
    private static final int EGG_REQUIRED_PROGRESS = 10;
    private static final List<String> SPECIAL_POKEMON_NAMES = Arrays.asList(
            "火凤凰",
            "潮汐海皇",
            "灵光鹿",
            "战神驼"
    );

    private final PokemonService pokemonService;
    private final TypeEffectivenessService typeEffectivenessService;
    private final BackpackRepository backpackRepository;
    private final PokedexRepository pokedexRepository;
    private final PokemonRepository pokemonRepository;
    private final StorageRepository storageRepository;
    private final PlayerEggRepository playerEggRepository;
    private final PlayerInventoryService playerInventoryService;
    private final PlayerHealingItemService playerHealingItemService;
    private final AchievementService achievementService;
    private final AuthService authService;
    private final GameSaveService gameSaveService;
    private final Random random = new Random();
    private final Map<Long, BattleState> battleStates = new ConcurrentHashMap<>();

    private enum BattleMode {
        WILD,
        TRAINER,
        DUNGEON
    }

    private static class BattleState {
        private final Long userId;
        private final BattleMode mode;
        private BackpackEntity currentPlayer;
        private PokemonEntity enemyPokemon;
        private boolean active;
        private boolean waitingForForcedSwitch;
        private final List<String> battleLog = new ArrayList<>();
        private final List<BackpackEntity> playerTeam = new ArrayList<>();
        private final List<PokemonEntity> enemyQueue = new ArrayList<>();
        private final Map<Long, TemporaryStatEffect> temporaryEffects = new HashMap<>();
        private int enemyIndex;
        private int enemyDefeatedCount;
        private int playerDefeatedCount;
        private String dungeonBossName;
        private int totalExpGained;
        private int lastExpGained;
        private int lastLevelBefore;
        private int lastLevelAfter;
        private boolean lastLevelUp;
        private int trainerDifficultyLevel;
        private int trainerRewardGold;

        private BattleState(Long userId, BattleMode mode) {
            this.userId = userId;
            this.mode = mode;
            this.active = true;
        }
    }

    private static class TemporaryStatEffect {
        private int attackDelta;
        private int defenseDelta;
        private int speedDelta;
        private int turnsRemaining;

        private boolean isEmpty() {
            return attackDelta == 0 && defenseDelta == 0 && speedDelta == 0;
        }
    }

    public GameController(PokemonService pokemonService,
                          BackpackRepository backpackRepository,
                          PokedexRepository pokedexRepository,
                          PokemonRepository pokemonRepository,
                          StorageRepository storageRepository,
                          PlayerEggRepository playerEggRepository,
                          PlayerInventoryService playerInventoryService,
                          PlayerHealingItemService playerHealingItemService,
                          AchievementService achievementService,
                          AuthService authService,
                          GameSaveService gameSaveService) {
        this.pokemonService = pokemonService;
        this.typeEffectivenessService = new TypeEffectivenessService();
        this.backpackRepository = backpackRepository;
        this.pokedexRepository = pokedexRepository;
        this.pokemonRepository = pokemonRepository;
        this.storageRepository = storageRepository;
        this.playerEggRepository = playerEggRepository;
        this.playerInventoryService = playerInventoryService;
        this.playerHealingItemService = playerHealingItemService;
        this.achievementService = achievementService;
        this.authService = authService;
        this.gameSaveService = gameSaveService;
    }

    @GetMapping("/game")
    public String gamePage(Model model, HttpSession session) {
        Long userId = getCurrentUserId(session);
        model.addAttribute("battleActive", getBattleState(userId).map(s -> s.active).orElse(false));
        return "game";
    }

    @GetMapping("/game/save/status")
    @ResponseBody
    public Map<String, Object> getSaveStatus(HttpSession session) {
        return gameSaveService.getSaveStatus(getCurrentUserId(session));
    }

    @PostMapping("/game/save/load")
    @ResponseBody
    public Map<String, Object> loadSave(HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (!gameSaveService.hasSaveData(userId)) {
            return fail("当前还没有可读取的存档");
        }
        Map<String, Object> result = successMessage("存档读取成功");
        result.putAll(gameSaveService.getSaveStatus(userId));
        return result;
    }

    @PostMapping("/game/save/new")
    @ResponseBody
    @Transactional
    public Map<String, Object> startNewGame(HttpSession session) {
        Long userId = getCurrentUserId(session);
        BattleState state = battleStates.get(userId);
        if (state != null) {
            cleanupBattleState(state, true, false);
        }
        gameSaveService.resetForNewGame(userId);
        Map<String, Object> result = successMessage("新游戏已创建，存档已经重置");
        result.putAll(gameSaveService.getSaveStatus(userId));
        return result;
    }

    @GetMapping("/wild-map")
    public String wildMapPage(HttpSession session) {
        getCurrentUserId(session);
        return "wild-map";
    }

    @GetMapping("/wild-battle")
    public String wildBattlePage(HttpSession session) {
        getCurrentUserId(session);
        return "wild-battle";
    }

    @PostMapping("/game/start")
    @ResponseBody
    @Transactional
    public Map<String, Object> startBattle(@RequestParam String pokemonName, HttpSession session) {
        return startWildBattleInternal(getCurrentUserId(session), pokemonName, null);
    }

    @PostMapping("/game/wild/start")
    @ResponseBody
    @Transactional
    public Map<String, Object> startWildBattle(@RequestParam String pokemonName,
                                               @RequestParam(required = false) String location,
                                               HttpSession session) {
        return startWildBattleInternal(getCurrentUserId(session), pokemonName, location);
    }

    @PostMapping("/game/attack")
    @ResponseBody
    @Transactional
    public Map<String, Object> attack(@RequestParam Long moveId, HttpSession session) {
        return performAttack(getCurrentUserId(session), moveId);
    }

    @PostMapping("/game/trainer/attack")
    @ResponseBody
    @Transactional
    public Map<String, Object> trainerAttack(@RequestParam Long moveId, HttpSession session) {
        return performAttack(getCurrentUserId(session), moveId);
    }

    @PostMapping("/game/dungeon/attack")
    @ResponseBody
    @Transactional
    public Map<String, Object> dungeonAttack(@RequestParam Long moveId, HttpSession session) {
        return performAttack(getCurrentUserId(session), moveId);
    }

    @GetMapping("/game/pokemons")
    @ResponseBody
    public List<Map<String, Object>> getStarterPokemons() {
        return pokemonService.getStarterPokemons().stream()
                .map(pokemon -> { Map<String, Object> item = toPokemonMap(pokemon); item.put("templatePreview", true); return item; })
                .collect(Collectors.toList());
    }

    @PostMapping("/game/select-starter")
    @ResponseBody
    @Transactional
    public Map<String, Object> selectStarter(@RequestParam String pokemonName, HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (backpackRepository.countByUserId(userId) > 0 || storageRepository.countByUserId(userId) > 0) {
            return fail("你已经拥有精灵了，不能重复选择初始精灵");
        }

        Optional<PokemonEntity> templateOpt = pokemonService.getStarterPokemons().stream()
                .filter(pokemon -> Objects.equals(pokemon.getName(), pokemonName))
                .findFirst();
        if (templateOpt.isEmpty()) {
            return fail("初始精灵不存在");
        }

        PokemonEntity starter = pokemonService.clonePokemonForReward(templateOpt.get());
        backpackRepository.save(new BackpackEntity(userId, starter));
        updatePokedex(userId, starter, true, false);
        return successMessage("初始精灵选择成功");
    }

    @GetMapping("/game/status")
    @ResponseBody
    public Map<String, Object> getStatus(HttpSession session) {
        BattleState state = battleStates.get(getCurrentUserId(session));
        Map<String, Object> result = new HashMap<>();
        result.put("battleActive", state != null && state.active);
        result.put("battleMode", state == null ? null : state.mode.name());
        return result;
    }

    @GetMapping("/game/backpack/pokemons")
    @ResponseBody
    public List<Map<String, Object>> getBackpackPokemons(HttpSession session) {
        return backpackRepository.findByUserIdOrderByCaughtTimeDesc(getCurrentUserId(session)).stream()
                 .map(BackpackEntity::getPokemon)
                .map(this::toPokemonMap)
                .collect(Collectors.toList());
    }

    @GetMapping("/game/backpack")
    @ResponseBody
    public List<Map<String, Object>> getBackpack(HttpSession session) {
        return backpackRepository.findByUserIdOrderByCaughtTimeDesc(getCurrentUserId(session)).stream()
                .map(this::toBackpackItemMap)
                .collect(Collectors.toList());
    }

    @GetMapping("/game/pokemon/detail")
    @ResponseBody
    public Map<String, Object> getPokemonDetail(@RequestParam Long pokemonId) {
        Optional<PokemonEntity> pokemonOpt = pokemonRepository.findById(pokemonId);
        if (pokemonOpt.isEmpty()) {
            return fail("精灵不存在");
        }
        Map<String, Object> result = successMessage("获取成功");
        result.put("pokemon", toPokemonDetailMap(pokemonOpt.get()));
        return result;
    }

    @GetMapping("/game/pokedex")
    @ResponseBody
    public List<Map<String, Object>> getPokedex(HttpSession session) {
        Long userId = getCurrentUserId(session);
        Map<String, Integer> rarityMap = getDistinctPokemonTemplates().stream()
                .collect(Collectors.toMap(PokemonEntity::getName, PokemonEntity::getRarity, (a, b) -> a));

        return pokedexRepository.findByUserIdOrderByPokemonNameAsc(userId).stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", entry.getId());
                    item.put("pokemonName", entry.getPokemonName());
                    item.put("pokemonType", entry.getPokemonType());
                    item.put("caught", entry.isCaught());
                    item.put("encounterCount", entry.getEncounterCount());
                    item.put("catchCount", entry.getCatchCount());
                    item.put("lotteryCount", entry.getLotteryCount());
                    item.put("rarity", rarityMap.getOrDefault(entry.getPokemonName(), 1));
                    item.put("description", entry.getDescription());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/game/catch")
    @ResponseBody
    @Transactional
    public Map<String, Object> catchPokemon(@RequestParam String ballType, HttpSession session) {
        Long userId = getCurrentUserId(session);
        BattleState state = battleStates.get(userId);
        if (state == null || !state.active || state.mode != BattleMode.WILD) {
            return fail("当前没有可捕获的野外战斗");
        }

        PokeBallType pokeBallType = parseBallType(ballType).orElse(null);
        if (pokeBallType == null) {
            return fail("精灵球类型无效");
        }
        if (!playerInventoryService.consumeBall(userId, pokeBallType)) {
            return fail("精灵球数量不足");
        }

        double catchRate = calculateCatchRate(pokeBallType, state.enemyPokemon);
        List<String> logs = new ArrayList<>(state.battleLog);
        logs.add("【玩家】使用了" + getBallName(pokeBallType));

        if (random.nextDouble() * 100 <= catchRate) {
            PokemonEntity enemy = state.enemyPokemon;
            fullHeal(enemy);
            persistBattlePokemonState(state);
            String placement = placeRewardPokemon(userId, enemy);
            updatePokedex(userId, enemy, true, false);
            addGold(userId, CATCH_SUCCESS_REWARD);
            addBreedingProgress(userId, BattleMode.WILD);
            cleanupBattleState(state, true, true);

            Map<String, Object> result = successMessage("捕获成功，获得 " + enemy.getName());
            result.put("battleOver", true);
            result.put("reward", CATCH_SUCCESS_REWARD);
            result.put("battleLog", logs);
            result.put("placement", placement);
            return result;
        }

        logs.add("【玩家】捕获失败");
        state.battleLog.clear();
        state.battleLog.addAll(logs);
        performEnemyTurn(state);
        persistBattlePokemonState(state);
        Map<String, Object> result = buildBattleResponse(state, false, null);
        result.put("success", false);
        return result;
    }

    @PostMapping("/game/escape")
    @ResponseBody
    @Transactional
    public Map<String, Object> escapeBattle(HttpSession session) {
        Long userId = getCurrentUserId(session);
        BattleState state = battleStates.get(userId);
        if (state == null || !state.active) {
            return fail("当前没有战斗");
        }
        if (state.mode != BattleMode.WILD) {
            return fail("当前战斗不能逃跑");
        }
        if (random.nextInt(100) < 75) {
            persistBattlePokemonState(state);
            cleanupBattleState(state, true, false);
            return successMessage("成功逃跑");
        }

        battleLog(state, "【系统】逃跑失败，野生精灵发动了反击");
        performEnemyTurn(state);
        persistBattlePokemonState(state);
        Map<String, Object> result = buildBattleResponse(state, false, null);
        result.put("success", false);
        result.put("message", "逃跑失败");
        return result;
    }

    @GetMapping("/game/battle/rates")
    @ResponseBody
    public Map<String, Object> getBattleRates(@RequestParam String ballType, HttpSession session) {
        BattleState state = battleStates.get(getCurrentUserId(session));
        Map<String, Object> result = new HashMap<>();
        Optional<PokeBallType> pokeBallType = parseBallType(ballType);
        result.put("catchRate", state == null || state.enemyPokemon == null || pokeBallType.isEmpty()
                ? 0.0
                : calculateCatchRate(pokeBallType.get(), state.enemyPokemon));
        result.put("escapeRate", state == null ? 100.0 : (state.mode == BattleMode.WILD ? 100.0 : 0.0));
        return result;
    }

    @PostMapping("/game/battle/switch")
    @ResponseBody
    @Transactional
    public Map<String, Object> switchPokemon(@RequestParam Long backpackId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        BattleState state = battleStates.get(userId);
        if (state == null || !state.active) {
            return fail("当前没有战斗");
        }

        BackpackEntity target = state.playerTeam.stream()
                .filter(item -> Objects.equals(item.getId(), backpackId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return fail("找不到可切换的精灵");
        }
        if (Objects.equals(state.currentPlayer.getId(), backpackId)) {
            return fail("当前就是这只精灵");
        }
        if (target.getPokemon().getCurrentHp() <= 0) {
            return fail("该精灵已经失去战斗能力");
        }

        state.currentPlayer = target;
        state.waitingForForcedSwitch = false;
        state.battleLog.add("【玩家】切换出了 " + target.getPokemon().getName());
        persistBattlePokemonState(state);
        return buildBattleResponse(state, false, null);
    }

    @GetMapping("/game/battle/switchable")
    @ResponseBody
    public List<Map<String, Object>> getSwitchablePokemons(HttpSession session) {
        BattleState state = battleStates.get(getCurrentUserId(session));
        if (state == null || !state.active) {
            return Collections.emptyList();
        }

        return state.playerTeam.stream()
                .filter(item -> !Objects.equals(item.getId(), state.currentPlayer.getId()))
                .filter(item -> item.getPokemon().getCurrentHp() > 0)
                .map(item -> {
                    PokemonEntity pokemon = item.getPokemon();
                    Map<String, Object> data = new HashMap<>();
                    data.put("backpackId", item.getId());
                    data.put("id", pokemon.getId());
                    data.put("name", pokemon.getName());
                    data.put("type", pokemon.getType().name());
                    data.put("level", pokemon.getLevel());
                    data.put("rarity", pokemon.getRarity());
                    data.put("currentHp", pokemon.getCurrentHp());
                    data.put("maxHp", pokemon.getMaxHp());
                    data.put("currentMp", pokemon.getCurrentMp());
                    data.put("maxMp", pokemon.getMaxMp());
                    return data;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/game/battle/use-item")
    @ResponseBody
    @Transactional
    public Map<String, Object> useBattleItem(@RequestParam String itemType, HttpSession session) {
        Long userId = getCurrentUserId(session);
        BattleState state = battleStates.get(userId);
        if (state == null || !state.active) {
            return fail("当前没有战斗");
        }

        HealingItemType healingItemType = parseHealingItemType(itemType).orElse(null);
        if (healingItemType == null) {
            return fail("道具类型无效");
        }
        if (!healingItemType.isBattleUsable()) {
            return fail("经验果只能在背包中使用");
        }
        if (!playerHealingItemService.consumeItem(userId, healingItemType)) {
            return fail("该道具数量不足");
        }

        PokemonEntity pokemon = state.currentPlayer.getPokemon();
        int oldHp = pokemon.getCurrentHp();
        int oldMp = pokemon.getCurrentMp();

        int hpRestore = healingItemType.getHpPercent() <= 0 ? 0 :
                Math.max(1, pokemon.getMaxHp() * healingItemType.getHpPercent() / 100);
        int mpRestore = healingItemType.getMpPercent() <= 0 ? 0 :
                Math.max(1, pokemon.getMaxMp() * healingItemType.getMpPercent() / 100);

        pokemon.setCurrentHp(Math.min(pokemon.getMaxHp(), pokemon.getCurrentHp() + hpRestore));
        pokemon.setCurrentMp(Math.min(pokemon.getMaxMp(), pokemon.getCurrentMp() + mpRestore));
        pokemon.setFainted(pokemon.getCurrentHp() <= 0);

        int actualHp = pokemon.getCurrentHp() - oldHp;
        int actualMp = pokemon.getCurrentMp() - oldMp;
        battleLog(state, "【道具】使用了" + getHealingItemName(healingItemType)
                + "，恢复 HP " + actualHp + "，恢复 MP " + actualMp);

        Map<String, Object> result = successMessage("道具使用成功");
        result.put("itemName", getHealingItemName(healingItemType));
        result.put("isCurrentPokemon", true);
        result.put("newHp", pokemon.getCurrentHp());
        result.put("newMp", pokemon.getCurrentMp());
        result.put("maxHp", pokemon.getMaxHp());
        result.put("maxMp", pokemon.getMaxMp());
        result.put("hpRestored", actualHp);
        result.put("mpRestored", actualMp);
        result.put("playerMoves", toMoveList(pokemon));
        result.put("battleLog", new ArrayList<>(state.battleLog));
        return result;
    }

    @GetMapping("/game/storage")
    @ResponseBody
    public Map<String, Object> getStorage(HttpSession session) {
        Long userId = getCurrentUserId(session);
        Map<String, Object> result = new HashMap<>();
        result.put("storage", storageRepository.findByUserIdOrderByStoredTimeDesc(userId).stream()
                .map(this::toStorageItemMap)
                .collect(Collectors.toList()));
        result.put("capacity", getStorageCapacity(userId));
        return result;
    }

    @PostMapping("/game/storage/deposit")
    @ResponseBody
    @Transactional
    public Map<String, Object> depositToStorage(@RequestParam Long backpackId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        Optional<BackpackEntity> backpackOpt = backpackRepository.findByIdAndUserId(backpackId, userId);
        if (backpackOpt.isEmpty()) {
            return fail("找不到背包中的精灵");
        }
        if (backpackRepository.countByUserId(userId) <= 1) {
            return fail("背包中至少要保留一只精灵");
        }
        if (storageRepository.countByUserId(userId) >= getStorageCapacity(userId)) {
            return fail("仓库空间不足");
        }
        if (isCurrentBattlePokemon(userId, backpackId)) {
            return fail("当前出战精灵不能移入仓库");
        }

        BackpackEntity backpack = backpackOpt.get();
        storageRepository.save(new StorageEntity(userId, backpack.getPokemon()));
        backpackRepository.delete(backpack);
        return successMessage("已移入仓库");
    }

    @PostMapping("/game/storage/withdraw")
    @ResponseBody
    @Transactional
    public Map<String, Object> withdrawFromStorage(@RequestParam Long storageId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        Optional<StorageEntity> storageOpt = storageRepository.findByIdAndUserId(storageId, userId);
        if (storageOpt.isEmpty()) {
            return fail("找不到仓库中的精灵");
        }
        if (backpackRepository.countByUserId(userId) >= BACKPACK_MAX_SIZE) {
            return fail("背包已满");
        }

        StorageEntity storage = storageOpt.get();
        backpackRepository.save(new BackpackEntity(userId, storage.getPokemon()));
        storageRepository.delete(storage);
        return successMessage("已移入背包");
    }

    @PostMapping("/game/storage/deposit-batch")
    @ResponseBody
    @Transactional
    public Map<String, Object> batchDeposit(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<Long> ids = extractIdList(body.get("pokemonIds"));
        if (ids.isEmpty()) {
            return fail("未选择精灵");
        }

        int success = 0;
        for (Long id : ids) {
            if (backpackRepository.countByUserId(userId) <= 1) {
                break;
            }
            if (storageRepository.countByUserId(userId) >= getStorageCapacity(userId)) {
                break;
            }
            Optional<BackpackEntity> backpackOpt = backpackRepository.findByIdAndUserId(id, userId);
            if (backpackOpt.isPresent() && !isCurrentBattlePokemon(userId, id)) {
                storageRepository.save(new StorageEntity(userId, backpackOpt.get().getPokemon()));
                backpackRepository.delete(backpackOpt.get());
                success++;
            }
        }

        return success == 0 ? fail("没有成功移入仓库的精灵") : successMessage("成功移入仓库 " + success + " 只精灵");
    }

    @PostMapping("/game/storage/withdraw-batch")
    @ResponseBody
    @Transactional
    public Map<String, Object> batchWithdraw(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<Long> ids = extractIdList(body.get("pokemonIds"));
        if (ids.isEmpty()) {
            return fail("未选择精灵");
        }

        int success = 0;
        for (Long id : ids) {
            if (backpackRepository.countByUserId(userId) >= BACKPACK_MAX_SIZE) {
                break;
            }
            Optional<StorageEntity> storageOpt = storageRepository.findByIdAndUserId(id, userId);
            if (storageOpt.isPresent()) {
                backpackRepository.save(new BackpackEntity(userId, storageOpt.get().getPokemon()));
                storageRepository.delete(storageOpt.get());
                success++;
            }
        }

        return success == 0 ? fail("没有成功移入背包的精灵") : successMessage("成功移入背包 " + success + " 只精灵");
    }

    @GetMapping("/game/inventory/status")
    @ResponseBody
    public Map<String, Object> getInventoryStatus(HttpSession session) {
        Long userId = getCurrentUserId(session);
        Map<String, Object> result = new HashMap<>();
        result.put("backpackCount", backpackRepository.countByUserId(userId));
        result.put("backpackMax", BACKPACK_MAX_SIZE);
        result.put("storageCount", storageRepository.countByUserId(userId));
        result.put("storageMax", getStorageCapacity(userId));
        result.put("gold", getPlayerGold(userId));
        return result;
    }

    @GetMapping("/game/breeding/status")
    @ResponseBody
    public Map<String, Object> getBreedingStatus(HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<BackpackEntity> backpack = backpackRepository.findByUserIdOrderByCaughtTimeDesc(userId);
        List<PlayerEgg> eggs = playerEggRepository.findByUserIdAndStatusInOrderByCreatedAtAsc(
                userId,
                List.of(EggStatus.INCUBATING, EggStatus.READY)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("cost", BREEDING_COST);
        result.put("maxEggs", MAX_ACTIVE_EGGS);
        result.put("currentEggs", eggs.size());
        result.put("canCreateEgg", eggs.size() < MAX_ACTIVE_EGGS);
        result.put("pokemons", backpack.stream().map(this::toBreedingCandidateMap).collect(Collectors.toList()));
        result.put("eggs", eggs.stream().map(this::toEggMap).collect(Collectors.toList()));
        return result;
    }

    @PostMapping("/game/breeding/start")
    @ResponseBody
    @Transactional
    public Map<String, Object> startBreeding(@RequestParam Long fatherId,
                                             @RequestParam Long motherId,
                                             HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (fatherId == motherId) {
            return fail("请选择两只不同的精灵进行孵蛋");
        }
        List<PlayerEgg> activeEggs = playerEggRepository.findByUserIdAndStatusInOrderByCreatedAtAsc(
                userId,
                List.of(EggStatus.INCUBATING, EggStatus.READY)
        );
        if (activeEggs.size() >= MAX_ACTIVE_EGGS) {
            return fail("孵蛋室已经满了，最多同时孵化 3 个蛋");
        }
        if (getPlayerGold(userId) < BREEDING_COST) {
            return fail("金币不足，孵蛋需要 " + BREEDING_COST + " 金币");
        }

        BackpackEntity fatherEntry = backpackRepository.findByIdAndUserId(fatherId, userId).orElse(null);
        BackpackEntity motherEntry = backpackRepository.findByIdAndUserId(motherId, userId).orElse(null);
        if (fatherEntry == null || motherEntry == null) {
            return fail("只能从背包中选择精灵进行孵蛋");
        }

        PokemonEntity father = fatherEntry.getPokemon();
        PokemonEntity mother = motherEntry.getPokemon();
        if (!pokemonService.canBreed(father, mother)) {
            return fail("只有同名且异性的普通精灵才能孵蛋");
        }

        PokemonEntity male = father.getGender() == PokemonGender.MALE ? father : mother;
        PokemonEntity female = father.getGender() == PokemonGender.FEMALE ? father : mother;
        addGold(userId, -BREEDING_COST);
        backpackRepository.delete(fatherEntry);
        backpackRepository.delete(motherEntry);
        PlayerEgg egg = new PlayerEgg(userId, male, female, father.getName() + "精灵蛋", father.getName(), EGG_REQUIRED_PROGRESS);
        playerEggRepository.save(egg);

        Map<String, Object> result = successMessage("孵蛋开始，前往战斗推进进度吧");
        result.put("egg", toEggMap(egg));
        result.put("gold", getPlayerGold(userId));
        return result;
    }

    @PostMapping("/game/breeding/hatch")
    @ResponseBody
    @Transactional
    public Map<String, Object> hatchEgg(@RequestParam Long eggId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        PlayerEgg egg = playerEggRepository.findByIdAndUserId(eggId, userId).orElse(null);
        if (egg == null) {
            return fail("找不到要孵化的精灵蛋");
        }
        if (egg.getStatus() != EggStatus.READY) {
            return fail("当前精灵蛋还没有准备好");
        }
        if (getAvailablePokemonSlots(userId) < 3) {
            return fail("空间不足，孵蛋结束时需要同时取回父母和新精灵，请至少预留 3 个位置");
        }

        List<com.example.demo.pokemon.enums.PokemonTalentType> inheritedTalents =
                pokemonService.rollInheritedTalents(egg.getFatherPokemon(), egg.getMotherPokemon());
        PokemonEntity child = pokemonService.createOffspringPokemon(egg.getTargetPokemonName(), inheritedTalents);
        String fatherPlacement = placePokemonWithoutSelling(userId, egg.getFatherPokemon());
        String motherPlacement = placePokemonWithoutSelling(userId, egg.getMotherPokemon());
        String childPlacement = placePokemonWithoutSelling(userId, child);
        if (fatherPlacement == null || motherPlacement == null || childPlacement == null) {
            pokemonService.deletePokemon(child.getId());
            return fail("空间不足，无法完成孵化，请先清理背包或仓库");
        }

        updatePokedex(userId, child, true, false);
        egg.setStatus(EggStatus.HATCHED);
        egg.setHatchedAt(java.time.LocalDateTime.now());
        playerEggRepository.save(egg);

        Map<String, Object> result = successMessage("孵化成功，获得了 " + child.getName());
        result.put("pokemon", toPokemonMap(child));
        result.put("placement", childPlacement);
        result.put("fatherPlacement", fatherPlacement);
        result.put("motherPlacement", motherPlacement);
        result.put("inheritedTalents", child.getTalentInfos().stream().map(talent -> talent.getName()).collect(Collectors.toList()));
        return result;
    }

    @PostMapping("/game/pokemon/evolve")
    @ResponseBody
    @Transactional
    public Map<String, Object> evolvePokemon(@RequestParam Long pokemonId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        PokemonEntity pokemon = findOwnedPokemon(userId, pokemonId).orElse(null);
        if (pokemon == null) {
            return fail("只能进化自己拥有的精灵");
        }
        String oldName = pokemon.getName();
        if (pokemonService.getEvolutionTargetName(pokemon).isEmpty()) {
            return fail("当前精灵还不能进化");
        }

        PokemonEntity evolved = pokemonService.evolvePokemon(pokemon);
        Map<String, Object> result = successMessage(oldName + " 已成功进化为 " + evolved.getName());
        result.put("pokemon", toPokemonMap(evolved));
        return result;
    }

    @PostMapping("/game/storage/expand")
    @ResponseBody
    @Transactional
    public Map<String, Object> expandStorage(HttpSession session) {
        Long userId = getCurrentUserId(session);
        int currentCapacity = getStorageCapacity(userId);
        if (currentCapacity >= STORAGE_MAX_LIMIT) {
            return fail("仓库已经扩充到上限");
        }
        if (getPlayerGold(userId) < STORAGE_EXPAND_COST) {
            return fail("金币不足");
        }

        addGold(userId, -STORAGE_EXPAND_COST);
        setStorageCapacity(userId, Math.min(STORAGE_MAX_LIMIT, currentCapacity + STORAGE_EXPAND_AMOUNT));
        return successMessage("仓库扩容成功");
    }

    @GetMapping("/game/gold")
    @ResponseBody
    public Map<String, Object> getGold(HttpSession session) {
        return Collections.singletonMap("gold", getPlayerGold(getCurrentUserId(session)));
    }

    @PostMapping("/game/gold/adjust")
    @ResponseBody
    @Transactional
    public Map<String, Object> adjustGold(@RequestParam int amount, HttpSession session) {
        Long userId = getCurrentUserId(session);
        int currentGold = getPlayerGold(userId);
        int newGold = Math.max(0, currentGold + amount);
        setPlayerGold(userId, newGold);

        Map<String, Object> result = successMessage("金币已调整");
        result.put("gold", newGold);
        result.put("change", newGold - currentGold);
        return result;
    }

    @PostMapping("/game/lottery")
    @ResponseBody
    @Transactional
    public Map<String, Object> lottery(HttpSession session) {
        Long userId = getCurrentUserId(session);
        int gold = getPlayerGold(userId);
        if (gold < LOTTERY_COST) {
            return fail("金币不足，无法抽奖");
        }
        addGold(userId, -LOTTERY_COST);

        int roll = random.nextInt(1000) + 1;
        Map<String, Object> result = successMessage("抽奖成功");
        result.put("reward", 0);

        if (roll <= 25) {
            Optional<PokemonEntity> opt = pokemonService.cloneGodCamelForLottery();
            if (opt.isPresent()) {
                placeRewardPokemon(userId, opt.get());
                updatePokedex(userId, opt.get(), false, true);
                result.put("prizeLevel", -1);
                result.put("prizeTitle", "特等奖");
                result.put("emoji", "✨");
                result.put("prizePokemon", opt.get().getName());
                result.put("prizePokemonType", getTypeName(opt.get().getType()));
            } else {
                addGold(userId, 1000);
                result.put("prizeLevel", 1);
                result.put("prizeTitle", "一等奖");
                result.put("emoji", "🏆");
                result.put("reward", 1000);
            }
        } else if (roll <= 50) {
            addGold(userId, 1000);
            result.put("prizeLevel", 1);
            result.put("prizeTitle", "一等奖");
            result.put("emoji", "🏆");
            result.put("reward", 1000);
        } else if (roll <= 100) {
            Optional<PokemonEntity> opt = pokemonService.getRandomNonSpecialPokemonForLottery();
            if (opt.isPresent()) {
                placeRewardPokemon(userId, opt.get());
                updatePokedex(userId, opt.get(), false, true);
                result.put("prizeLevel", 2);
                result.put("prizeTitle", "二等奖");
                result.put("emoji", "👑");
                result.put("prizePokemon", opt.get().getName());
                result.put("prizePokemonType", getTypeName(opt.get().getType()));
            } else {
                addGold(userId, 250);
                result.put("prizeLevel", 3);
                result.put("prizeTitle", "三等奖");
                result.put("emoji", "🥉");
                result.put("reward", 250);
            }
        } else if (roll <= 200) {
            addGold(userId, 250);
            result.put("prizeLevel", 3);
            result.put("prizeTitle", "三等奖");
            result.put("emoji", "🥉");
            result.put("reward", 250);
        } else if (roll <= 350) {
            addGold(userId, 100);
            result.put("prizeLevel", 4);
            result.put("prizeTitle", "普通奖");
            result.put("emoji", "🎁");
            result.put("reward", 100);
        } else {
            addGold(userId, 50);
            result.put("prizeLevel", 0);
            result.put("prizeTitle", "安慰奖");
            result.put("emoji", "🙂");
            result.put("reward", 50);
        }

        result.put("gold", getPlayerGold(userId));
        return result;
    }

    @GetMapping("/game/shop/items")
    @ResponseBody
    public Map<String, Object> getShopItems() {
        Map<String, Object> result = new HashMap<>();
        result.put("pokeBalls", Arrays.stream(PokeBallType.values()).map(type -> {
            Map<String, Object> item = new HashMap<>();
            item.put("type", type.name());
            item.put("name", getBallName(type));
            item.put("price", type.getPrice());
            return item;
        }).collect(Collectors.toList()));
        result.put("healingItems", Arrays.stream(HealingItemType.values()).map(type -> {
            Map<String, Object> item = new HashMap<>();
            item.put("type", type.name());
            item.put("name", getHealingItemName(type));
            item.put("price", type.getPrice());
            item.put("description", getHealingItemDescription(type));
            return item;
        }).collect(Collectors.toList()));
        return result;
    }

    @PostMapping("/game/shop/buy")
    @ResponseBody
    @Transactional
    public Map<String, Object> buyShopItem(@RequestParam(required = false) String ballType,
                                           @RequestParam(required = false) String itemType,
                                           @RequestParam int quantity,
                                           @RequestParam String category,
                                           HttpSession session) {
        Long userId = getCurrentUserId(session);
        if (quantity <= 0) {
            return fail("购买数量无效");
        }

        String normalizedCategory = category.toUpperCase(Locale.ROOT);
        int cost;
        if ("POKEBALL".equals(normalizedCategory)) {
            PokeBallType type = parseBallType(ballType).orElse(null);
            if (type == null) {
                return fail("精灵球类型无效");
            }
            cost = type.getPrice() * quantity;
            if (getPlayerGold(userId) < cost) {
                return fail("金币不足");
            }
            addGold(userId, -cost);
            playerInventoryService.addBalls(userId, type, quantity);
            return successMessage("购买成功");
        }

        if ("HEALING".equals(normalizedCategory)) {
            HealingItemType type = parseHealingItemType(itemType).orElse(null);
            if (type == null) {
                return fail("恢复道具类型无效");
            }
            cost = type.getPrice() * quantity;
            if (getPlayerGold(userId) < cost) {
                return fail("金币不足");
            }
            addGold(userId, -cost);
            playerHealingItemService.addItems(userId, type, quantity);
            return successMessage("购买成功");
        }

        return fail("购买分类无效");
    }

    @GetMapping("/game/inventory")
    @ResponseBody
    public Map<String, Object> getInventory(HttpSession session) {
        Long userId = getCurrentUserId(session);
        Map<String, Object> result = new HashMap<>();
        result.put("pokeBalls", playerInventoryService.getPlayerInventoryMap(userId));
        result.put("healingItems", playerHealingItemService.getPlayerInventoryMap(userId));
        result.put("gold", getPlayerGold(userId));
        return result;
    }
    @PostMapping("/game/backpack/use-item")
    @ResponseBody
    @Transactional
    public Map<String, Object> useBackpackItem(@RequestParam Long backpackId,
                                               @RequestParam String itemType,
                                               HttpSession session) {
        Long userId = getCurrentUserId(session);
        BackpackEntity backpack = backpackRepository.findByIdAndUserId(backpackId, userId).orElse(null);
        if (backpack == null) {
            return fail("找不到要使用道具的精灵");
        }

        HealingItemType type = parseHealingItemType(itemType).orElse(null);
        if (type == null) {
            return fail("道具类型无效");
        }
        if (!playerHealingItemService.consumeItem(userId, type)) {
            return fail("该道具数量不足");
        }

        PokemonEntity pokemon = backpack.getPokemon();
        int oldLevel = pokemon.getLevel();
        int oldHp = pokemon.getCurrentHp();
        int oldMp = pokemon.getCurrentMp();
        int actualHp = 0;
        int actualMp = 0;
        int levelGain = applyBackpackItemEffect(type, pokemon, oldLevel);

        if (!type.isExperienceFruit()) {
            actualHp = pokemon.getCurrentHp() - oldHp;
            actualMp = pokemon.getCurrentMp() - oldMp;
        }

        pokemonRepository.save(pokemon);
        Map<String, Object> result = successMessage(type.isExperienceFruit()
                ? pokemon.getName() + " 使用了" + getHealingItemName(type) + "，提升到 Lv." + pokemon.getLevel()
                : pokemon.getName() + " 使用了" + getHealingItemName(type));
        result.put("pokemon", toPokemonMap(pokemon));
        result.put("itemName", getHealingItemName(type));
        result.put("isExperienceFruit", type.isExperienceFruit());
        result.put("levelGain", levelGain);
        result.put("oldLevel", oldLevel);
        result.put("newLevel", pokemon.getLevel());
        result.put("hpRestored", actualHp);
        result.put("mpRestored", actualMp);
        result.put("remaining", playerHealingItemService.getItemCount(userId, type));
        return result;
    }

    @PostMapping("/game/backpack/use-item-batch")
    @ResponseBody
    @Transactional
    public Map<String, Object> useBackpackItemBatch(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        HealingItemType type = parseHealingItemType(String.valueOf(body.get("itemType"))).orElse(null);
        if (type == null) {
            return fail("道具类型无效");
        }
        if (!type.isExperienceFruit()) {
            return fail("当前只支持经验果批量使用");
        }

        Long backpackId = null;
        Object backpackIdRaw = body.get("backpackId");
        if (backpackIdRaw instanceof Number number) {
            backpackId = number.longValue();
        } else if (backpackIdRaw != null) {
            try {
                backpackId = Long.parseLong(String.valueOf(backpackIdRaw));
            } catch (NumberFormatException ignored) {
            }
        }
        if (backpackId == null) {
            return fail("请先选择要使用的精灵");
        }

        int quantity = 1;
        Object quantityRaw = body.get("quantity");
        if (quantityRaw instanceof Number number) {
            quantity = number.intValue();
        } else if (quantityRaw != null) {
            try {
                quantity = Integer.parseInt(String.valueOf(quantityRaw));
            } catch (NumberFormatException ignored) {
            }
        }
        quantity = Math.max(1, quantity);

        BackpackEntity backpack = backpackRepository.findByIdAndUserId(backpackId, userId).orElse(null);
        if (backpack == null) {
            return fail("\u627e\u4e0d\u5230\u8981\u4f7f\u7528\u7ecf\u9a8c\u679c\u7684\u7cbe\u7075");
        }
        if (!playerHealingItemService.hasEnoughItems(userId, type, quantity)) {
            return fail(getHealingItemName(type) + " 数量不足");
        }

        PokemonEntity pokemon = backpack.getPokemon();
        int oldLevel = pokemon.getLevel();
        int usedCount = 0;
        int totalLevelGain = 0;
        while (usedCount < quantity && playerHealingItemService.consumeItem(userId, type)) {
            totalLevelGain += applyBackpackItemEffect(type, pokemon, pokemon.getLevel());
            usedCount++;
            if (pokemon.getLevel() >= PokemonEntity.MAX_LEVEL) {
                break;
            }
        }
        pokemonRepository.save(pokemon);

        if (usedCount == 0) {
            return fail("没有成功使用经验果");
        }

        Map<String, Object> result = successMessage(pokemon.getName() + " 一次使用了 " + usedCount + " 个" + getHealingItemName(type) + "，当前等级 Lv." + pokemon.getLevel());
        result.put("pokemon", toPokemonMap(pokemon));
        result.put("usedCount", usedCount);
        result.put("count", usedCount);
        result.put("oldLevel", oldLevel);
        result.put("newLevel", pokemon.getLevel());
        result.put("totalLevelGain", totalLevelGain);
        result.put("remaining", playerHealingItemService.getItemCount(userId, type));
        return result;
    }

    @PostMapping("/game/shop/sell")
    @ResponseBody
    @Transactional
    public Map<String, Object> sellPokemon(@RequestParam String source,
                                           @RequestParam Long id,
                                           HttpSession session) {
        Long userId = getCurrentUserId(session);
        String normalizedSource = source.toLowerCase(Locale.ROOT);

        if ("backpack".equals(normalizedSource)) {
            Optional<BackpackEntity> backpackOpt = backpackRepository.findByIdAndUserId(id, userId);
            if (backpackOpt.isEmpty()) {
                return fail("找不到要贩卖的精灵");
            }
            if (backpackRepository.countByUserId(userId) <= 1) {
                return fail("至少要保留一只精灵在背包中");
            }
            if (isCurrentBattlePokemon(userId, id)) {
                return fail("当前出战精灵不能贩卖");
            }

            BackpackEntity backpack = backpackOpt.get();
            int reward = calculateSellPrice(backpack.getPokemon());
            backpackRepository.delete(backpack);
            pokemonService.deletePokemon(backpack.getPokemon().getId());
            addGold(userId, reward);
            return successMessage("成功贩卖，获得 " + reward + " 金币");
        }

        if ("storage".equals(normalizedSource)) {
            Optional<StorageEntity> storageOpt = storageRepository.findByIdAndUserId(id, userId);
            if (storageOpt.isEmpty()) {
                return fail("找不到要贩卖的精灵");
            }
            StorageEntity storage = storageOpt.get();
            int reward = calculateSellPrice(storage.getPokemon());
            storageRepository.delete(storage);
            pokemonService.deletePokemon(storage.getPokemon().getId());
            addGold(userId, reward);
            return successMessage("成功贩卖，获得 " + reward + " 金币");
        }

        return fail("来源类型无效");
    }

    @PostMapping("/game/gold/init")
    @ResponseBody
    @Transactional
    public Map<String, Object> initGold(HttpSession session) {
        Long userId = getCurrentUserId(session);
        setPlayerGold(userId, INITIAL_GOLD);
        return successMessage("金币已重置");
    }

    @GetMapping("/game/backpack/status")
    @ResponseBody
    public List<Map<String, Object>> getBackpackStatus(HttpSession session) {
        return backpackRepository.findByUserIdOrderByCaughtTimeDesc(getCurrentUserId(session)).stream()
                .map(item -> {
                    PokemonEntity pokemon = item.getPokemon();
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", item.getId());
                    data.put("pokemonId", pokemon.getId());
                    data.put("name", pokemon.getName());
                    data.put("type", pokemon.getType().name());
                    data.put("level", pokemon.getLevel());
                    data.put("currentHp", pokemon.getCurrentHp());
                    data.put("maxHp", pokemon.getMaxHp());
                    data.put("currentMp", pokemon.getCurrentMp());
                    data.put("maxMp", pokemon.getMaxMp());
                    data.put("rarity", pokemon.getRarity());
                    return data;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/game/hospital/heal")
    @ResponseBody
    @Transactional
    public Map<String, Object> healAll(HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<BackpackEntity> backpack = backpackRepository.findByUserIdOrderByCaughtTimeDesc(userId);
        List<BackpackEntity> needHeal = backpack.stream()
                .filter(item -> item.getPokemon().getCurrentHp() < item.getPokemon().getMaxHp()
                        || item.getPokemon().getCurrentMp() < item.getPokemon().getMaxMp())
                .collect(Collectors.toList());

        if (needHeal.isEmpty()) {
            return fail("没有需要治疗的精灵");
        }
        int cost = needHeal.size() * HOSPITAL_HEAL_COST;
        if (getPlayerGold(userId) < cost) {
            return fail("金币不足");
        }

        addGold(userId, -cost);
        needHeal.forEach(item -> fullHeal(item.getPokemon()));
        return successMessage("治疗完成，共花费 " + cost + " 金币");
    }

    @PostMapping("/game/hospital/heal-selected")
    @ResponseBody
    @Transactional
    public Map<String, Object> healSelected(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<Long> ids = extractIdList(body.get("pokemonIds"));
        if (ids.isEmpty()) {
            return fail("未选择精灵");
        }

        List<BackpackEntity> selected = ids.stream()
                .map(id -> backpackRepository.findByIdAndUserId(id, userId).orElse(null))
                .filter(Objects::nonNull)
                .filter(item -> item.getPokemon().getCurrentHp() < item.getPokemon().getMaxHp()
                        || item.getPokemon().getCurrentMp() < item.getPokemon().getMaxMp())
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            return fail("所选精灵都不需要治疗");
        }
        int cost = selected.size() * HOSPITAL_HEAL_COST;
        if (getPlayerGold(userId) < cost) {
            return fail("金币不足");
        }

        addGold(userId, -cost);
        selected.forEach(item -> fullHeal(item.getPokemon()));
        return successMessage("治疗完成，共花费 " + cost + " 金币");
    }

    @GetMapping("/game/trainer/check")
    @ResponseBody
    public Map<String, Object> checkTrainerBattle(HttpSession session) {
        Long userId = getCurrentUserId(session);
        long count = backpackRepository.countByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("eligible", count >= TRAINER_BATTLE_POKEMON_COUNT);
        result.put("currentCount", count);
        result.put("requiredCount", TRAINER_BATTLE_POKEMON_COUNT);
        return result;
    }

    @PostMapping("/game/trainer/start")
    @ResponseBody
    @Transactional
    public Map<String, Object> startTrainerBattle(@RequestParam String pokemonName,
                                                  @RequestParam(defaultValue = "30") int difficultyLevel,
                                                  HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<BackpackEntity> team = getAvailableBackpackPokemons(userId);
        if (team.size() < TRAINER_BATTLE_POKEMON_COUNT) {
            return fail("训练师对战需要至少 6 只背包精灵");
        }

        BackpackEntity starter = selectBackpackPokemonByName(userId, pokemonName).orElse(null);
        if (starter == null || starter.getPokemon().getCurrentHp() <= 0) {
            return fail("请选择可出战的精灵");
        }

        int normalizedDifficulty = normalizeTrainerDifficulty(difficultyLevel);
        BattleState state = new BattleState(userId, BattleMode.TRAINER);
        state.playerTeam.addAll(team);
        state.currentPlayer = starter;
        state.trainerDifficultyLevel = normalizedDifficulty;
        state.trainerRewardGold = getTrainerRewardByDifficulty(normalizedDifficulty);
        state.enemyQueue.addAll(buildTrainerEnemyQueue(team, normalizedDifficulty));
        state.enemyPokemon = state.enemyQueue.get(0);
        state.battleLog.add("【系统】训练师对战开始，挑战难度 Lv." + normalizedDifficulty);
        state.battleLog.add("【系统】胜利奖励：" + state.trainerRewardGold + " 金币");
        state.battleLog.add("【敌方】派出了 " + state.enemyPokemon.getName());
        battleStates.put(userId, state);
        persistBattlePokemonState(state);
        return buildBattleResponse(state, false, null);
    }

    @PostMapping("/game/dungeon/start")
    @ResponseBody
    @Transactional
    public Map<String, Object> startDungeonBattle(@RequestParam String pokemonName,
                                                  @RequestParam String bossName,
                                                  HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<BackpackEntity> team = getAvailableBackpackPokemons(userId);
        if (team.size() < TRAINER_BATTLE_POKEMON_COUNT) {
            return fail("副本挑战需要至少 6 只背包精灵");
        }

        BackpackEntity starter = selectBackpackPokemonByName(userId, pokemonName).orElse(null);
        if (starter == null || starter.getPokemon().getCurrentHp() <= 0) {
            return fail("请选择可出战的精灵");
        }

        PokemonEntity bossTemplate = findTemplateByName(bossName).orElse(null);
        if (bossTemplate == null) {
            return fail("副本 BOSS 不存在");
        }

        BattleState state = new BattleState(userId, BattleMode.DUNGEON);
        state.playerTeam.addAll(team);
        state.currentPlayer = starter;
        state.dungeonBossName = bossName;
        for (int i = 0; i < TRAINER_BATTLE_POKEMON_COUNT; i++) {
            PokemonEntity enemy = pokemonService.clonePokemonForBattle(bossTemplate);
            scaleBattlePokemon(enemy, 60);
            state.enemyQueue.add(enemy);
        }
        state.enemyPokemon = state.enemyQueue.get(0);
        state.battleLog.add("【系统】副本挑战开始");
        state.battleLog.add("【敌方】出现了 " + bossName);
        battleStates.put(userId, state);

        Map<String, Object> result = buildBattleResponse(state, false, null);
        result.put("isDungeonBattle", true);
        result.put("dungeonBoss", bossName);
        return result;
    }

    @GetMapping("/game/achievements")
    @ResponseBody
    public Map<String, Object> getAchievements(HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<PlayerAchievement> completed = achievementService.getPlayerAchievements(userId);
        Map<AchievementType, PlayerAchievement> completedMap = completed.stream()
                .collect(Collectors.toMap(PlayerAchievement::getAchievementType, item -> item, (a, b) -> a));

        List<Map<String, Object>> achievements = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            PlayerAchievement record = completedMap.get(type);
            Map<String, Object> item = new HashMap<>();
            item.put("id", record == null ? null : record.getId());
            item.put("type", type.name());
            item.put("name", getAchievementName(type));
            item.put("description", getAchievementDescription(type));
            item.put("icon", getAchievementIcon(type));
            item.put("goldReward", type.getGoldReward());
            item.put("masterBallReward", type.getMasterBallReward());
            item.put("completed", record != null);
            item.put("rewardClaimed", record != null && record.isRewardClaimed());
            achievements.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stats", achievementService.getAchievementStats(userId));
        result.put("achievements", achievements);
        return result;
    }

    @PostMapping("/game/achievements/claim")
    @ResponseBody
    @Transactional
    public Map<String, Object> claimAchievement(@RequestParam Long achievementId, HttpSession session) {
        Long userId = getCurrentUserId(session);
        Map<String, Object> result = achievementService.claimAchievementReward(userId, achievementId, getPlayerGold(userId));
        if (Boolean.TRUE.equals(result.get("success"))) {
            int goldReward = ((Number) result.getOrDefault("goldReward", 0)).intValue();
            if (goldReward > 0) {
                addGold(userId, goldReward);
            }
        }
        return result;
    }

    @GetMapping("/game/achievements/check")
    @ResponseBody
    @Transactional
    public Map<String, Object> checkAchievements(HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<PlayerAchievement> newAchievements = achievementService.checkAchievements(userId, getPlayerGold(userId));
        Map<String, Object> result = new HashMap<>();
        result.put("count", newAchievements.size());
        result.put("newAchievements", newAchievements.stream().map(item -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", item.getId());
            data.put("name", getAchievementName(item.getAchievementType()));
            return data;
        }).collect(Collectors.toList()));
        return result;
    }

    private Map<String, Object> startWildBattleInternal(Long userId, String pokemonName, String location) {
        BackpackEntity starter = selectBackpackPokemonByName(userId, pokemonName).orElse(null);
        if (starter == null) {
            return fail("找不到出战精灵");
        }
        if (starter.getPokemon().getCurrentHp() <= 0) {
            return fail("该精灵当前无法出战");
        }

        PokemonEntity enemyTemplate = pickWildEnemy(location).orElse(null);
        if (enemyTemplate == null) {
            return fail("未找到野生精灵");
        }

        PokemonEntity enemy = pokemonService.clonePokemonForBattle(enemyTemplate);
        scaleBattlePokemon(enemy, determineWildEnemyLevel(starter.getPokemon(), location));
        updatePokedex(userId, enemy, false, false);

        BattleState state = new BattleState(userId, BattleMode.WILD);
        state.currentPlayer = starter;
        state.playerTeam.addAll(getAvailableBackpackPokemons(userId));
        state.enemyPokemon = enemy;
        state.battleLog.add("【系统】战斗开始");
        state.battleLog.add("【敌方】野生精灵 " + enemy.getName() + " 出现了");
        battleStates.put(userId, state);
        return buildBattleResponse(state, false, null);
    }

    private Map<String, Object> performAttack(Long userId, Long moveId) {
        BattleState state = battleStates.get(userId);
        if (state == null || !state.active) {
            return fail("战斗不存在");
        }
        if (state.waitingForForcedSwitch) {
            return fail("请先更换倒下的精灵");
        }

        PokemonEntity player = state.currentPlayer.getPokemon();
        Optional<PokemonMoveEntity> moveOpt = pokemonService.getPokemonMoves(player.getId()).stream()
                .filter(move -> Objects.equals(move.getMove().getId(), moveId))
                .findFirst();
        if (moveOpt.isEmpty()) {
            return fail("技能不存在");
        }

        MoveEntity move = moveOpt.get().getMove();
        if (player.getCurrentMp() < move.getMpCost()) {
            return fail("MP 不足");
        }

        // 速度判断：速度高的一方先出手，相等时随机
        int playerSpeed = getEffectiveSpeed(state, player);
        int enemySpeed = getEffectiveSpeed(state, state.enemyPokemon);
        boolean playerFirst;
        if (playerSpeed > enemySpeed) {
            playerFirst = true;
            battleLog(state, "【系统】" + player.getName() + " 速度更快（" + playerSpeed + " > " + enemySpeed + "），先手出击！");
        } else if (enemySpeed > playerSpeed) {
            playerFirst = false;
            battleLog(state, "【系统】" + state.enemyPokemon.getName() + " 速度更快（" + enemySpeed + " > " + playerSpeed + "），先手出击！");
        } else {
            playerFirst = random.nextBoolean();
            battleLog(state, "【系统】双方速度相同（" + playerSpeed + "），" + (playerFirst ? player.getName() : state.enemyPokemon.getName()) + " 抢先出手！");
        }

        if (playerFirst) {
            // 玩家先出手
            player.setCurrentMp(player.getCurrentMp() - move.getMpCost());
            executeMove(state, true, player, state.enemyPokemon, move);
            persistBattlePokemonState(state);
            if (state.enemyPokemon.getCurrentHp() <= 0) {
                return handleEnemyDefeated(state);
            }
            // 敌方后手
            performEnemyTurn(state);
            persistBattlePokemonState(state);
        } else {
            // 敌方先出手
            performEnemyTurn(state);
            persistBattlePokemonState(state);
            // 若玩家精灵倒下（等待换精灵或战斗结束），本回合玩家攻击取消（不扣MP）
            if (!state.active || state.waitingForForcedSwitch) {
                return buildBattleResponse(state, !state.active, null);
            }
            // 玩家后手出击
            player.setCurrentMp(player.getCurrentMp() - move.getMpCost());
            executeMove(state, true, player, state.enemyPokemon, move);
            persistBattlePokemonState(state);
            if (state.enemyPokemon.getCurrentHp() <= 0) {
                return handleEnemyDefeated(state);
            }
        }

        if (!state.active) {
            return buildBattleResponse(state, true, null);
        }
        return buildBattleResponse(state, false, null);
    }

    private void performEnemyTurn(BattleState state) {
        if (!state.active || state.enemyPokemon == null || state.enemyPokemon.getCurrentHp() <= 0) {
            return;
        }

        PokemonEntity enemy = state.enemyPokemon;
        List<PokemonMoveEntity> affordableMoves = pokemonService.getPokemonMoves(enemy.getId()).stream()
                .filter(move -> enemy.getCurrentMp() >= move.getMove().getMpCost())
                .collect(Collectors.toList());
        List<PokemonMoveEntity> moves = affordableMoves.isEmpty() ? pokemonService.getPokemonMoves(enemy.getId()) : affordableMoves;
        if (moves.isEmpty()) {
            return;
        }

        PokemonMoveEntity chosen = pickEnemyMove(enemy, moves);
        MoveEntity move = chosen.getMove();
        if (enemy.getCurrentMp() >= move.getMpCost()) {
            enemy.setCurrentMp(enemy.getCurrentMp() - move.getMpCost());
        }

        executeMove(state, false, enemy, state.currentPlayer.getPokemon(), move);
        decayTemporaryEffects(state);
        if (state.currentPlayer.getPokemon().getCurrentHp() > 0) {
            persistBattlePokemonState(state);
            return;
        }

        state.playerDefeatedCount++;
        if (hasAvailableSwitch(state)) {
            state.waitingForForcedSwitch = true;
            persistBattlePokemonState(state);
            battleLog(state, "【系统】" + state.currentPlayer.getPokemon().getName() + " 失去战斗能力，请更换下一只精灵");
            return;
        }

        state.active = false;
        persistBattlePokemonState(state);
        battleLog(state, "【系统】你的精灵全部失去战斗能力，战斗结束");
        addBreedingProgress(state.userId, state.mode);
        cleanupBattleState(state, true, false);
    }

    private PokemonMoveEntity pickEnemyMove(PokemonEntity enemy, List<PokemonMoveEntity> moves) {
        List<PokemonMoveEntity> healingMoves = moves.stream()
                .filter(pm -> pm.getMove().getEffectType() == MoveEffectType.HEAL_SELF)
                .toList();
        if (enemy.getCurrentHp() <= Math.max(1, enemy.getMaxHp() / 3) && !healingMoves.isEmpty()) {
            return healingMoves.get(random.nextInt(healingMoves.size()));
        }

        List<PokemonMoveEntity> damageMoves = moves.stream()
                .filter(pm -> pm.getMove().getEffectType() == MoveEffectType.DAMAGE)
                .toList();
        if (!damageMoves.isEmpty() && random.nextInt(100) < 70) {
            return damageMoves.get(random.nextInt(damageMoves.size()));
        }
        return moves.get(random.nextInt(moves.size()));
    }

    private Map<String, Object> handleEnemyDefeated(BattleState state) {
        PokemonEntity defeatedEnemy = state.enemyPokemon;
        state.enemyDefeatedCount++;
        battleLog(state, "【敌方】" + defeatedEnemy.getName() + " 倒下了");
        awardEnemyExperience(state, defeatedEnemy);

        if (state.mode == BattleMode.WILD) {
            addGold(state.userId, WILD_BATTLE_REWARD);
            persistBattlePokemonState(state);
            state.active = false;
            addBreedingProgress(state.userId, BattleMode.WILD);
            cleanupBattleState(state, true, false);
            return buildBattleResponse(state, true, WILD_BATTLE_REWARD);
        }

        deleteTempEnemy(defeatedEnemy);
        state.enemyIndex++;
        if (state.enemyIndex >= state.enemyQueue.size()) {
            awardBattleClearBonus(state);
            int reward = state.mode == BattleMode.DUNGEON ? DUNGEON_CLEAR_REWARD : getTrainerRewardForState(state);
            addGold(state.userId, reward);
            persistBattlePokemonState(state);
            state.active = false;
            addBreedingProgress(state.userId, state.mode);

            Map<String, Object> result = buildBattleResponse(state, true, reward);
            if (state.mode == BattleMode.DUNGEON) {
                result.put("winner", "PLAYER");
                result.put("rewardGold", reward);
                PokemonEntity rewardPokemon = findTemplateByName(state.dungeonBossName)
                        .map(pokemonService::clonePokemonForReward)
                        .orElse(null);
                if (rewardPokemon != null) {
                    placeRewardPokemon(state.userId, rewardPokemon);
                    updatePokedex(state.userId, rewardPokemon, true, false);
                    result.put("rewardPokemon", rewardPokemon.getName());
                }
            }
            cleanupBattleState(state, true, false);
            return result;
        }

        state.enemyPokemon = state.enemyQueue.get(state.enemyIndex);
        battleLog(state, "【敌方】派出了下一只精灵 " + state.enemyPokemon.getName());
        return buildBattleResponse(state, false, null);
    }

    private void awardEnemyExperience(BattleState state, PokemonEntity defeatedEnemy) {
        if (state == null || state.currentPlayer == null || state.currentPlayer.getPokemon() == null || defeatedEnemy == null) {
            return;
        }
        int exp = calculateEnemyExperience(state.mode, defeatedEnemy);
        grantExperience(state, exp, defeatedEnemy.getName() + " 被击败");
    }

    private void awardBattleClearBonus(BattleState state) {
        if (state == null || state.mode == BattleMode.WILD) {
            return;
        }
        int averageLevel = calculateAverageTeamLevel(state.playerTeam);
        int bonus = state.mode == BattleMode.DUNGEON
                ? DUNGEON_CLEAR_EXP_BONUS + averageLevel * 4
                : TRAINER_CLEAR_EXP_BONUS + averageLevel * 3;
        String source = state.mode == BattleMode.DUNGEON ? "副本通关" : "训练师连胜";
        grantExperience(state, bonus, source + "奖励");
    }

    private void grantExperience(BattleState state, int exp, String source) {
        if (state == null || state.currentPlayer == null || state.currentPlayer.getPokemon() == null) {
            return;
        }
        int actualExp = Math.max(0, exp);
        if (actualExp == 0) {
            return;
        }

        PokemonEntity player = state.currentPlayer.getPokemon();
        PokemonEntity.ExperienceGainResult gain = player.gainExperience(actualExp);
        state.totalExpGained += gain.getGainedExp();
        state.lastExpGained = gain.getGainedExp();
        state.lastLevelBefore = gain.getOldLevel();
        state.lastLevelAfter = gain.getNewLevel();
        state.lastLevelUp = gain.isLeveledUp();

        battleLog(state, "【成长】" + player.getName() + " 因 " + source + " 获得 " + gain.getGainedExp() + " 点经验");
        if (gain.isLeveledUp()) {
            battleLog(state, "【成长】" + player.getName() + " 升到了 Lv." + gain.getNewLevel() + "，HP 和 MP 已恢复");
        } else if (player.getLevel() < PokemonEntity.MAX_LEVEL) {
            battleLog(state, "【成长】距离下一级还需 " + player.getExpToNextLevel() + " 点经验");
        } else {
            battleLog(state, "【成长】" + player.getName() + " 已达到满级 Lv.60");
        }
        persistBattlePokemonState(state);
    }

    private int calculateEnemyExperience(BattleMode mode, PokemonEntity enemy) {
        int levelFactor = enemy.getLevel();
        int rarityFactor = enemy.getRarity();
        return switch (mode) {
            case TRAINER -> TRAINER_EXP_BASE + levelFactor * 5 + rarityFactor * 8;
            case DUNGEON -> DUNGEON_EXP_BASE + levelFactor * 6 + rarityFactor * 10;
            default -> WILD_EXP_BASE + levelFactor * 4 + rarityFactor * 6;
        };
    }

    private int calculateAverageTeamLevel(Collection<BackpackEntity> team) {
        if (team == null || team.isEmpty()) {
            return 1;
        }
        return Math.max(1, (int) Math.round(team.stream()
                .map(BackpackEntity::getPokemon)
                .filter(Objects::nonNull)
                .mapToInt(PokemonEntity::getLevel)
                .average()
                .orElse(1.0)));
    }

    private int clampBattleLevel(int level) {
        return Math.max(1, Math.min(PokemonEntity.MAX_LEVEL, level));
    }

    private void scaleBattlePokemon(PokemonEntity pokemon, int targetLevel) {
        if (pokemon == null) {
            return;
        }
        pokemon.scaleToLevel(clampBattleLevel(targetLevel));
        pokemonRepository.save(pokemon);
    }

    private int applyBackpackItemEffect(HealingItemType type, PokemonEntity pokemon, int oldLevel) {
        if (type.isExperienceFruit()) {
            int targetLevel = Math.min(PokemonEntity.MAX_LEVEL, oldLevel + type.getLevelGain());
            pokemon.scaleToLevel(targetLevel);
            return pokemon.getLevel() - oldLevel;
        }

        int hpRestore = type.getHpPercent() <= 0 ? 0 : Math.max(1, pokemon.getMaxHp() * type.getHpPercent() / 100);
        int mpRestore = type.getMpPercent() <= 0 ? 0 : Math.max(1, pokemon.getMaxMp() * type.getMpPercent() / 100);
        pokemon.setCurrentHp(Math.min(pokemon.getMaxHp(), pokemon.getCurrentHp() + hpRestore));
        pokemon.setCurrentMp(Math.min(pokemon.getMaxMp(), pokemon.getCurrentMp() + mpRestore));
        pokemon.setFainted(pokemon.getCurrentHp() <= 0);
        return 0;
    }

    private int normalizeTrainerDifficulty(int difficultyLevel) {
        if (difficultyLevel >= 50) {
            return 50;
        }
        if (difficultyLevel >= 30) {
            return 30;
        }
        return 10;
    }

    private int getTrainerRewardByDifficulty(int difficultyLevel) {
        return switch (normalizeTrainerDifficulty(difficultyLevel)) {
            case 50 -> 2000;
            case 30 -> 1000;
            default -> 500;
        };
    }

    private int getTrainerRewardForState(BattleState state) {
        return state != null && state.trainerRewardGold > 0 ? state.trainerRewardGold : TRAINER_BATTLE_REWARD;
    }
    private int determineWildEnemyLevel(PokemonEntity starter, String location) {
        int baseLevel = starter == null ? 1 : starter.getLevel();
        int locationModifier = switch (location == null ? "" : location.toLowerCase(Locale.ROOT)) {
            case "plain" -> -2;
            case "volcano" -> 1;
            case "forest", "coast", "valley" -> 0;
            default -> 0;
        };
        int variance = random.nextInt(3) - 1;
        return clampBattleLevel(baseLevel + locationModifier + variance);
    }

    private int determineTrainerEnemyLevel(List<BackpackEntity> team, int slotIndex) {
        int averageLevel = calculateAverageTeamLevel(team);
        return clampBattleLevel(Math.max(6, averageLevel + slotIndex / 2));
    }

    private int determineDungeonEnemyLevel(List<BackpackEntity> team, int slotIndex, PokemonEntity bossTemplate) {
        int averageLevel = calculateAverageTeamLevel(team);
        int rarityBonus = bossTemplate == null ? 0 : Math.max(0, bossTemplate.getRarity() - 3);
        return clampBattleLevel(Math.max(10, averageLevel + 2 + slotIndex / 2 + rarityBonus));
    }

    private void executeMove(BattleState state,
                             boolean playerAttack,
                             PokemonEntity attacker,
                             PokemonEntity defender,
                             MoveEntity move) {
        MoveEffectType effectType = move.getEffectType() == null ? MoveEffectType.DAMAGE : move.getEffectType();
        boolean hit = random.nextInt(100) < move.getAccuracy();
        String side = playerAttack ? "\u3010\u73a9\u5bb6\u3011" : "\u3010\u654c\u65b9\u3011";
        if (!hit) {
            battleLog(state, side + attacker.getName() + " \u4f7f\u7528 " + move.getName() + " \u4f46\u6ca1\u6709\u547d\u4e2d");
            return;
        }

        switch (effectType) {
            case HEAL_SELF -> applyHealMove(state, side, attacker, move);
            case BUFF_ATTACK, BUFF_DEFENSE, BUFF_SPEED -> applyBuffMove(state, side, attacker, move);
            case DEBUFF_ATTACK, DEBUFF_DEFENSE, DEBUFF_SPEED -> applyDebuffMove(state, side, attacker, defender, move);
            case DAMAGE -> attackTarget(state, playerAttack, attacker, defender, move);
        }
    }

    private void applyHealMove(BattleState state, String side, PokemonEntity attacker, MoveEntity move) {
        int healAmount = Math.max(1, attacker.getMaxHp() * Math.max(1, move.getEffectValue()) / 100);
        int healed = Math.min(attacker.getMaxHp() - attacker.getCurrentHp(), healAmount);
        attacker.setCurrentHp(Math.min(attacker.getMaxHp(), attacker.getCurrentHp() + healAmount));
        attacker.setFainted(false);
        battleLog(state, side + attacker.getName() + " \u4f7f\u7528 " + move.getName() + "\uff0c\u6062\u590d\u4e86 " + Math.max(0, healed) + " \u70b9\u751f\u547d");
    }

    private void applyBuffMove(BattleState state, String side, PokemonEntity attacker, MoveEntity move) {
        TemporaryStatEffect effect = getTemporaryEffect(state, attacker);
        switch (move.getEffectType()) {
            case BUFF_ATTACK -> effect.attackDelta += move.getEffectValue();
            case BUFF_DEFENSE -> effect.defenseDelta += move.getEffectValue();
            case BUFF_SPEED -> effect.speedDelta += move.getEffectValue();
            default -> {
            }
        }
        effect.turnsRemaining = Math.max(effect.turnsRemaining, Math.max(1, move.getEffectDuration()));
        battleLog(state, side + attacker.getName() + " \u4f7f\u7528 " + move.getName() + "\uff0c" + describeEffectChange(move));
    }

    private void applyDebuffMove(BattleState state, String side, PokemonEntity attacker, PokemonEntity defender, MoveEntity move) {
        TemporaryStatEffect effect = getTemporaryEffect(state, defender);
        switch (move.getEffectType()) {
            case DEBUFF_ATTACK -> effect.attackDelta -= move.getEffectValue();
            case DEBUFF_DEFENSE -> effect.defenseDelta -= move.getEffectValue();
            case DEBUFF_SPEED -> effect.speedDelta -= move.getEffectValue();
            default -> {
            }
        }
        effect.turnsRemaining = Math.max(effect.turnsRemaining, Math.max(1, move.getEffectDuration()));
        battleLog(state, side + attacker.getName() + " \u4f7f\u7528 " + move.getName() + "\uff0c\u4f7f " + defender.getName() + " " + describeEffectChange(move));
    }

    private TemporaryStatEffect getTemporaryEffect(BattleState state, PokemonEntity pokemon) {
        return state.temporaryEffects.computeIfAbsent(pokemon.getId(), id -> new TemporaryStatEffect());
    }

    private void decayTemporaryEffects(BattleState state) {
        List<Long> expired = new ArrayList<>();
        for (Map.Entry<Long, TemporaryStatEffect> entry : state.temporaryEffects.entrySet()) {
            TemporaryStatEffect effect = entry.getValue();
            if (effect.turnsRemaining > 0) {
                effect.turnsRemaining--;
            }
            if (effect.turnsRemaining <= 0 || effect.isEmpty()) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(state.temporaryEffects::remove);
    }

    private int getEffectiveSpeed(BattleState state, PokemonEntity pokemon) {
        TemporaryStatEffect effect = state.temporaryEffects.get(pokemon.getId());
        return Math.max(1, pokemon.getSpeed() + (effect == null ? 0 : effect.speedDelta));
    }

    private int getEffectiveAttack(BattleState state, PokemonEntity pokemon) {
        TemporaryStatEffect effect = state.temporaryEffects.get(pokemon.getId());
        return Math.max(1, pokemon.getAttack() + (effect == null ? 0 : effect.attackDelta));
    }

    private int getEffectiveDefense(BattleState state, PokemonEntity pokemon) {
        TemporaryStatEffect effect = state.temporaryEffects.get(pokemon.getId());
        return Math.max(1, pokemon.getDefense() + (effect == null ? 0 : effect.defenseDelta));
    }

    private String describeEffectChange(MoveEntity move) {
        String durationText = move.getEffectDuration() > 0 ? "\uff0c\u6301\u7eed " + move.getEffectDuration() + " \u56de\u5408" : "";
        return switch (move.getEffectType()) {
            case BUFF_ATTACK -> "\u653b\u51fb\u63d0\u5347 " + move.getEffectValue() + durationText;
            case BUFF_DEFENSE -> "\u9632\u5fa1\u63d0\u5347 " + move.getEffectValue() + durationText;
            case BUFF_SPEED -> "\u901f\u5ea6\u63d0\u5347 " + move.getEffectValue() + durationText;
            case DEBUFF_ATTACK -> "\u653b\u51fb\u4e0b\u964d " + move.getEffectValue() + durationText;
            case DEBUFF_DEFENSE -> "\u9632\u5fa1\u4e0b\u964d " + move.getEffectValue() + durationText;
            case DEBUFF_SPEED -> "\u901f\u5ea6\u4e0b\u964d " + move.getEffectValue() + durationText;
            case HEAL_SELF -> "\u6062\u590d\u751f\u547d";
            case DAMAGE -> "\u9020\u6210\u4f24\u5bb3";
        };
    }

    private List<String> describeBattleBuffs(BattleState state, PokemonEntity pokemon) {
        TemporaryStatEffect effect = state.temporaryEffects.get(pokemon.getId());
        if (effect == null || effect.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> buffs = new ArrayList<>();
        if (effect.attackDelta != 0) {
            buffs.add("\u653b\u51fb" + (effect.attackDelta > 0 ? "+" : "") + effect.attackDelta + " (" + effect.turnsRemaining + "\u56de\u5408)");
        }
        if (effect.defenseDelta != 0) {
            buffs.add("\u9632\u5fa1" + (effect.defenseDelta > 0 ? "+" : "") + effect.defenseDelta + " (" + effect.turnsRemaining + "\u56de\u5408)");
        }
        if (effect.speedDelta != 0) {
            buffs.add("\u901f\u5ea6" + (effect.speedDelta > 0 ? "+" : "") + effect.speedDelta + " (" + effect.turnsRemaining + "\u56de\u5408)");
        }
        return buffs;
    }

    private String describeMove(MoveEntity move) {
        MoveEffectType effectType = move.getEffectType() == null ? MoveEffectType.DAMAGE : move.getEffectType();
        return switch (effectType) {
            case HEAL_SELF -> "\u6062\u590d\u81ea\u8eab " + move.getEffectValue() + "% HP";
            case BUFF_ATTACK -> "\u81ea\u8eab\u653b\u51fb +" + move.getEffectValue() + "\uff0c\u6301\u7eed " + move.getEffectDuration() + " \u56de\u5408";
            case BUFF_DEFENSE -> "\u81ea\u8eab\u9632\u5fa1 +" + move.getEffectValue() + "\uff0c\u6301\u7eed " + move.getEffectDuration() + " \u56de\u5408";
            case BUFF_SPEED -> "\u81ea\u8eab\u901f\u5ea6 +" + move.getEffectValue() + "\uff0c\u6301\u7eed " + move.getEffectDuration() + " \u56de\u5408";
            case DEBUFF_ATTACK -> "\u654c\u65b9\u653b\u51fb -" + move.getEffectValue() + "\uff0c\u6301\u7eed " + move.getEffectDuration() + " \u56de\u5408";
            case DEBUFF_DEFENSE -> "\u654c\u65b9\u9632\u5fa1 -" + move.getEffectValue() + "\uff0c\u6301\u7eed " + move.getEffectDuration() + " \u56de\u5408";
            case DEBUFF_SPEED -> "\u654c\u65b9\u901f\u5ea6 -" + move.getEffectValue() + "\uff0c\u6301\u7eed " + move.getEffectDuration() + " \u56de\u5408";
            case DAMAGE -> "\u5a01\u529b " + move.getPower() + " | \u547d\u4e2d " + move.getAccuracy() + "%";
        };
    }

    private void attackTarget(BattleState state,
                              boolean playerAttack,
                              PokemonEntity attacker,
                              PokemonEntity defender,
                              MoveEntity move) {
        double effectiveness = typeEffectivenessService.getEffectiveness(move.getType(), defender.getType());
        String side = playerAttack ? "\u3010\u73a9\u5bb6\u3011" : "\u3010\u654c\u65b9\u3011";

        double baseDamage = move.getPower() * 0.22
                + getEffectiveAttack(state, attacker) * 0.38
                + attacker.getLevel() * 0.8
                - getEffectiveDefense(state, defender) * 0.32;
        double randomFactor = 0.88 + (random.nextDouble() * 0.18);
        int damage = Math.max(1, (int) Math.round(Math.max(1.0, baseDamage) * effectiveness * randomFactor));
        defender.setCurrentHp(Math.max(0, defender.getCurrentHp() - damage));
        defender.setFainted(defender.getCurrentHp() <= 0);

        String effectText = effectiveness > 1.0 ? "\uff0c\u6548\u679c\u62d4\u7fa4" : (effectiveness < 1.0 ? "\uff0c\u6548\u679c\u8f83\u5f31" : "");
        battleLog(state, side + attacker.getName() + " \u4f7f\u7528 " + move.getName() + "\uff0c\u9020\u6210 " + damage + " \u70b9\u4f24\u5bb3" + effectText);
    }

    private Map<String, Object> buildBattleResponse(BattleState state, boolean battleOver, Integer reward) {
        Map<String, Object> result = new HashMap<>();
        PokemonEntity playerPokemon = state.currentPlayer.getPokemon();
        result.put("success", true);
        result.put("playerPokemon", toBattlePokemonMap(playerPokemon));
        result.put("enemyPokemon", state.enemyPokemon == null ? null : toBattlePokemonMap(state.enemyPokemon));
        result.put("playerMoves", toMoveList(playerPokemon));
        result.put("battleLog", new ArrayList<>(state.battleLog));
        result.put("playerHp", playerPokemon.getCurrentHp());
        result.put("playerMaxHp", playerPokemon.getMaxHp());
        result.put("playerMp", playerPokemon.getCurrentMp());
        result.put("playerMaxMp", playerPokemon.getMaxMp());
        result.put("playerLevel", playerPokemon.getLevel());
        result.put("playerExperience", playerPokemon.getExperience());
        result.put("playerExpProgress", playerPokemon.getExpProgressInCurrentLevel());
        result.put("playerExpRequired", playerPokemon.getExpRequiredForCurrentLevel());
        result.put("playerExpToNext", playerPokemon.getExpToNextLevel());
        result.put("enemyHp", state.enemyPokemon == null ? 0 : state.enemyPokemon.getCurrentHp());
        result.put("enemyMaxHp", state.enemyPokemon == null ? 0 : state.enemyPokemon.getMaxHp());
        result.put("enemyMp", state.enemyPokemon == null ? 0 : state.enemyPokemon.getCurrentMp());
        result.put("enemyMaxMp", state.enemyPokemon == null ? 0 : state.enemyPokemon.getMaxMp());
        result.put("battleOver", battleOver || !state.active);
        result.put("forceSwitch", state.waitingForForcedSwitch);
        result.put("lastExpGained", state.lastExpGained);
        result.put("totalExpGained", state.totalExpGained);
        result.put("lastLevelBefore", state.lastLevelBefore == 0 ? playerPokemon.getLevel() : state.lastLevelBefore);
        result.put("lastLevelAfter", state.lastLevelAfter == 0 ? playerPokemon.getLevel() : state.lastLevelAfter);
        result.put("lastLevelUp", state.lastLevelUp);
        result.put("playerEffectiveness", state.enemyPokemon == null ? 1.0 :
                typeEffectivenessService.getEffectiveness(playerPokemon.getType(), state.enemyPokemon.getType()));
        result.put("enemyEffectiveness", state.enemyPokemon == null ? 1.0 :
                typeEffectivenessService.getEffectiveness(state.enemyPokemon.getType(), playerPokemon.getType()));
        result.put("playerBattleBuffs", describeBattleBuffs(state, playerPokemon));
        result.put("enemyBattleBuffs", state.enemyPokemon == null ? Collections.emptyList() : describeBattleBuffs(state, state.enemyPokemon));

        if (state.mode == BattleMode.TRAINER) {
            result.put("isTrainerBattle", true);
            result.put("trainerDifficultyLevel", state.trainerDifficultyLevel);
            result.put("trainerRewardGold", getTrainerRewardForState(state));
        }
        if (state.mode != BattleMode.WILD) {
            result.put("enemyRemaining", Math.max(0, state.enemyQueue.size() - state.enemyDefeatedCount));
            result.put("playerRemaining", countAlive(state.playerTeam));
            result.put("enemyDefeated", state.enemyDefeatedCount);
            result.put("playerDefeated", state.playerDefeatedCount);
        }
        if (state.mode == BattleMode.DUNGEON) {
            result.put("isDungeonBattle", true);
            result.put("dungeonBoss", state.dungeonBossName);
        }
        if (reward != null) {
            result.put("reward", reward);
        }
        if (battleOver || !state.active || reward != null) {
            result.put("winner", countAlive(state.playerTeam) > 0 ? "PLAYER" : "ENEMY");
        }
        return result;
    }

    private Map<String, Object> toPokemonMap(PokemonEntity pokemon) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", pokemon.getId());
        data.put("name", pokemon.getName());
        data.put("type", pokemon.getType().name());
        data.put("rarity", pokemon.getRarity());
        data.put("level", pokemon.getLevel());
        data.put("experience", pokemon.getExperience());
        data.put("expProgress", pokemon.getExpProgressInCurrentLevel());
        data.put("expRequired", pokemon.getExpRequiredForCurrentLevel());
        data.put("expToNext", pokemon.getExpToNextLevel());
        data.put("maxLevel", PokemonEntity.MAX_LEVEL);
        data.put("maxHp", pokemon.getMaxHp());
        data.put("currentHp", pokemon.getCurrentHp());
        data.put("maxMp", pokemon.getMaxMp());
        data.put("currentMp", pokemon.getCurrentMp());
        data.put("attack", pokemon.getAttack());
        data.put("defense", pokemon.getDefense());
        data.put("speed", pokemon.getSpeed());
        data.put("talents", pokemon.getTalentInfos());
        data.put("talentCount", pokemon.getTalentInfos().size());
        data.put("talentNames", pokemon.getTalentInfos().stream().map(talent -> talent.getName()).collect(Collectors.toList()));
        data.put("gender", pokemon.getGender() == null ? PokemonGender.UNKNOWN.name() : pokemon.getGender().name());
        data.put("genderLabel", getGenderLabel(pokemon.getGender()));
        return data;
    }

    private Map<String, Object> toPokemonDetailMap(PokemonEntity pokemon) {
        Map<String, Object> data = toPokemonMap(pokemon);
        data.put("moves", toMoveList(pokemon));
        data.put("fainted", pokemon.isFainted());
        return data;
    }

    private Map<String, Object> toBattlePokemonMap(PokemonEntity pokemon) {
        return toPokemonMap(pokemon);
    }

    private Map<String, Object> toBackpackItemMap(BackpackEntity item) {
        PokemonEntity pokemon = item.getPokemon();
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("pokemonId", pokemon.getId());
        data.put("pokemonName", pokemon.getName());
        data.put("pokemonType", pokemon.getType().name());
        data.put("rarity", pokemon.getRarity());
        data.put("level", pokemon.getLevel());
        data.put("currentHp", pokemon.getCurrentHp());
        data.put("maxHp", pokemon.getMaxHp());
        data.put("currentMp", pokemon.getCurrentMp());
        data.put("maxMp", pokemon.getMaxMp());
        data.put("attack", pokemon.getAttack());
        data.put("defense", pokemon.getDefense());
        data.put("speed", pokemon.getSpeed());
        data.put("talents", pokemon.getTalentInfos());
        data.put("talentNames", pokemon.getTalentInfos().stream().map(talent -> talent.getName()).collect(Collectors.toList()));
        data.put("gender", pokemon.getGender() == null ? PokemonGender.UNKNOWN.name() : pokemon.getGender().name());
        data.put("genderLabel", getGenderLabel(pokemon.getGender()));
        data.put("caughtTime", item.getCaughtTime());
        data.put("used", item.isUsed());
        data.put("source", "backpack");
        return data;
    }

    private Map<String, Object> toStorageItemMap(StorageEntity item) {
        PokemonEntity pokemon = item.getPokemon();
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("pokemonId", pokemon.getId());
        data.put("pokemonName", pokemon.getName());
        data.put("pokemonType", pokemon.getType().name());
        data.put("rarity", pokemon.getRarity());
        data.put("level", pokemon.getLevel());
        data.put("talents", pokemon.getTalentInfos());
        data.put("talentNames", pokemon.getTalentInfos().stream().map(talent -> talent.getName()).collect(Collectors.toList()));
        data.put("gender", pokemon.getGender() == null ? PokemonGender.UNKNOWN.name() : pokemon.getGender().name());
        data.put("genderLabel", getGenderLabel(pokemon.getGender()));
        data.put("storedTime", item.getStoredTime());
        data.put("source", "storage");
        return data;
    }

    private Map<String, Object> toBreedingCandidateMap(BackpackEntity item) {
        Map<String, Object> data = toBackpackItemMap(item);
        PokemonEntity pokemon = item.getPokemon();
        data.put("backpackId", item.getId());
        data.put("breedable", pokemon != null
                && !pokemonService.isSpecialPokemon(pokemon.getName())
                && pokemon.getGender() != null
                && pokemon.getGender() != PokemonGender.UNKNOWN);
        return data;
    }

    private Map<String, Object> toEggMap(PlayerEgg egg) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", egg.getId());
        data.put("eggName", egg.getEggName());
        data.put("targetPokemonName", egg.getTargetPokemonName());
        data.put("currentProgress", egg.getCurrentProgress());
        data.put("requiredProgress", egg.getRequiredProgress());
        data.put("status", egg.getStatus().name());
        data.put("statusLabel", getEggStatusLabel(egg.getStatus()));
        data.put("fatherName", egg.getFatherPokemon() == null ? "--" : egg.getFatherPokemon().getName());
        data.put("motherName", egg.getMotherPokemon() == null ? "--" : egg.getMotherPokemon().getName());
        data.put("createdAt", egg.getCreatedAt());
        return data;
    }

    private List<Map<String, Object>> toMoveList(PokemonEntity pokemon) {
        return pokemonService.getPokemonMoves(pokemon.getId()).stream()
                .map(PokemonMoveEntity::getMove)
                .limit(4)
                .map(move -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", move.getId());
                    data.put("name", move.getName());
                    data.put("power", move.getPower());
                    data.put("accuracy", move.getAccuracy());
                    data.put("mpCost", move.getMpCost());
                    data.put("type", move.getType().name());
                    data.put("effectType", move.getEffectType() == null ? MoveEffectType.DAMAGE.name() : move.getEffectType().name());
                    data.put("effectValue", move.getEffectValue());
                    data.put("effectDuration", move.getEffectDuration());
                    data.put("description", describeMove(move));
                    data.put("category", move.getEffectType() == null || move.getEffectType() == MoveEffectType.DAMAGE ? "damage" : "support");
                    return data;
                })
                .collect(Collectors.toList());
    }

    private void updatePokedex(Long userId, PokemonEntity pokemon, boolean caught, boolean lottery) {
        PokedexEntity entry = pokedexRepository.findByUserIdAndPokemonName(userId, pokemon.getName()).orElseGet(() -> {
            PokedexEntity created = new PokedexEntity();
            created.setUserId(userId);
            created.setPokemonName(pokemon.getName());
            created.setPokemonType(getTypeName(pokemon.getType()));
            created.setDescription(pokemon.getName() + " 的图鉴记录");
            return created;
        });
        entry.recordEncounter();
        if (lottery) {
            entry.recordLottery();
        } else if (caught) {
            entry.recordCatch();
        }
        pokedexRepository.save(entry);
    }

    private String placeRewardPokemon(Long userId, PokemonEntity pokemon) {
        if (backpackRepository.countByUserId(userId) < BACKPACK_MAX_SIZE) {
            backpackRepository.save(new BackpackEntity(userId, pokemon));
            return "backpack";
        }
        if (storageRepository.countByUserId(userId) < getStorageCapacity(userId)) {
            storageRepository.save(new StorageEntity(userId, pokemon));
            return "storage";
        }
        pokemonService.deletePokemon(pokemon.getId());
        addGold(userId, 200);
        return "sold";
    }

    private List<PokemonEntity> buildTrainerEnemyQueue(List<BackpackEntity> playerTeam, int difficultyLevel) {
        List<PokemonEntity> templates = getDistinctPokemonTemplates().stream()
                .filter(pokemon -> pokemon.getRarity() >= 1)
                .filter(pokemon -> !SPECIAL_POKEMON_NAMES.contains(pokemon.getName()))
                .sorted(Comparator.comparingInt(PokemonEntity::getRarity).reversed())
                .limit(12)
                .collect(Collectors.toList());
        if (templates.isEmpty()) {
            return new ArrayList<>();
        }

        Collections.shuffle(templates, random);
        List<PokemonEntity> result = new ArrayList<>();
        for (int i = 0; i < TRAINER_BATTLE_POKEMON_COUNT; i++) {
            PokemonEntity template = templates.get(i % templates.size());
            PokemonEntity enemy = pokemonService.clonePokemonForBattle(template);
            scaleBattlePokemon(enemy, difficultyLevel);
            result.add(enemy);
        }
        return result;
    }

    private Optional<PokemonEntity> pickWildEnemy(String location) {
        List<PokemonEntity> templates = getDistinctPokemonTemplates();
        if (templates.isEmpty()) {
            return Optional.empty();
        }

        String normalized = location == null ? "" : location.toLowerCase(Locale.ROOT);
        List<PokemonEntity> filtered = templates.stream()
                .filter(pokemon -> isPokemonAllowedForLocation(pokemon, normalized))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            filtered = templates;
        }
        return Optional.of(filtered.get(random.nextInt(filtered.size())));
    }

    private boolean isPokemonAllowedForLocation(PokemonEntity pokemon, String location) {
        if (SPECIAL_POKEMON_NAMES.contains(pokemon.getName())) {
            return false;
        }
        if ("plain".equals(location)) {
            return pokemon.getRarity() <= 2;
        }
        if ("volcano".equals(location)) {
            return pokemon.getType() == PokemonType.FIRE;
        }
        if ("coast".equals(location)) {
            return pokemon.getType() == PokemonType.WATER;
        }
        if ("forest".equals(location)) {
            return pokemon.getType() == PokemonType.GRASS;
        }
        if ("valley".equals(location)) {
            return pokemon.getType() == PokemonType.NORMAL;
        }
        return true;
    }

    private List<PokemonEntity> getDistinctPokemonTemplates() {
        Map<String, PokemonEntity> map = new LinkedHashMap<>();
        for (PokemonEntity pokemon : pokemonService.getAllPokemons()) {
            map.putIfAbsent(pokemon.getName(), pokemon);
        }
        return new ArrayList<>(map.values());
    }

    private Optional<PokemonEntity> findTemplateByName(String name) {
        return getDistinctPokemonTemplates().stream()
                .filter(pokemon -> Objects.equals(pokemon.getName(), name))
                .findFirst();
    }

    private List<BackpackEntity> getAvailableBackpackPokemons(Long userId) {
        return backpackRepository.findByUserIdOrderByCaughtTimeDesc(userId);
    }

    private Optional<BackpackEntity> selectBackpackPokemonByName(Long userId, String pokemonName) {
        return backpackRepository.findByUserIdOrderByCaughtTimeDesc(userId).stream()
                .filter(item -> Objects.equals(item.getPokemon().getName(), pokemonName))
                .findFirst();
    }

    private Optional<BattleState> getBattleState(Long userId) {
        return Optional.ofNullable(battleStates.get(userId));
    }

    private boolean hasAvailableSwitch(BattleState state) {
        return state.playerTeam.stream()
                .anyMatch(item -> !Objects.equals(item.getId(), state.currentPlayer.getId())
                        && item.getPokemon().getCurrentHp() > 0);
    }

    private boolean isCurrentBattlePokemon(Long userId, Long backpackId) {
        BattleState state = battleStates.get(userId);
        return state != null
                && state.active
                && state.currentPlayer != null
                && Objects.equals(state.currentPlayer.getId(), backpackId);
    }

    private int countAlive(Collection<BackpackEntity> team) {
        return (int) team.stream().filter(item -> item.getPokemon().getCurrentHp() > 0).count();
    }

    private void cleanupBattleState(BattleState state, boolean removeState, boolean keepEnemyPokemon) {
        if (!keepEnemyPokemon) {
            deleteTempEnemy(state.enemyPokemon);
        }
        cleanupEnemyQueue(state);
        state.active = false;
        if (removeState) {
            battleStates.remove(state.userId);
        }
    }

    private void cleanupEnemyQueue(BattleState state) {
        for (PokemonEntity enemy : state.enemyQueue) {
            if (enemy != null && state.enemyPokemon != null && Objects.equals(enemy.getId(), state.enemyPokemon.getId())) {
                continue;
            }
            deleteTempEnemy(enemy);
        }
    }

    private void deleteTempEnemy(PokemonEntity enemy) {
        if (enemy == null || enemy.getId() == null) {
            return;
        }
        try {
            pokemonService.deletePokemon(enemy.getId());
        } catch (Exception ignored) {
        }
    }

    private double calculateCatchRate(PokeBallType ballType, PokemonEntity enemy) {
        double hpRatio = enemy.getMaxHp() == 0 ? 1.0 : (double) enemy.getCurrentHp() / enemy.getMaxHp();
        double rarityPenalty = Math.max(0, enemy.getRarity() - 1) * 4.0;
        double rate = ballType.getBaseCatchRate() + (1.0 - hpRatio) * 60.0 - rarityPenalty;
        return Math.max(3.0, Math.min(100.0, rate));
    }

    private int calculateSellPrice(PokemonEntity pokemon) {
        return switch (pokemon.getRarity()) {
            case 2 -> 100;
            case 3 -> 150;
            case 4 -> 200;
            case 5 -> 250;
            default -> 50;
        };
    }

    private void persistBattlePokemonState(BattleState state) {
        if (state == null || state.currentPlayer == null || state.currentPlayer.getPokemon() == null) {
            return;
        }
        pokemonRepository.save(state.currentPlayer.getPokemon());
    }

    private void fullHeal(PokemonEntity pokemon) {
        pokemon.setCurrentHp(pokemon.getMaxHp());
        pokemon.setCurrentMp(pokemon.getMaxMp());
        pokemon.setFainted(false);
    }

    private void battleLog(BattleState state, String message) {
        state.battleLog.add(message);
    }

    private Long getCurrentUserId(HttpSession session) {
        authService.ensureLocalUser();
        return DEFAULT_USER_ID;
    }

    private int getPlayerGold(Long userId) {
        return authService.getUserById(userId).map(user -> user.getGold()).orElse(INITIAL_GOLD);
    }

    private void setPlayerGold(Long userId, int gold) {
        authService.updateUserGold(userId, gold);
    }

    private void addGold(Long userId, int amount) {
        setPlayerGold(userId, Math.max(0, getPlayerGold(userId) + amount));
    }

    private int getStorageCapacity(Long userId) {
        return authService.getUserById(userId).map(user -> user.getStorageCapacity()).orElse(STORAGE_BASE_SIZE);
    }

    private void setStorageCapacity(Long userId, int value) {
        authService.updateUserStorageCapacity(userId, value);
    }

    private Optional<PokeBallType> parseBallType(String ballType) {
        try {
            return Optional.ofNullable(ballType).map(type -> PokeBallType.valueOf(type.toUpperCase(Locale.ROOT)));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<HealingItemType> parseHealingItemType(String itemType) {
        try {
            return Optional.ofNullable(itemType).map(type -> HealingItemType.valueOf(type.toUpperCase(Locale.ROOT)));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private List<Long> extractIdList(Object raw) {
        if (!(raw instanceof List<?> rawList)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Number number) {
                ids.add(number.longValue());
            } else if (item instanceof String text && !text.isBlank()) {
                ids.add(Long.parseLong(text));
            }
        }
        return ids;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> successMessage(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    private String getTypeName(PokemonType type) {
        return switch (type) {
            case FIRE -> "火系";
            case WATER -> "水系";
            case GRASS -> "草系";
            default -> "普通系";
        };
    }

    private String getBallName(PokeBallType type) {
        return switch (type) {
            case MEDIUM -> "中级精灵球";
            case ADVANCED -> "高级精灵球";
            case MASTER -> "大师球";
            default -> "初级精灵球";
        };
    }

    private String getHealingItemName(HealingItemType type) {
        return switch (type) {
            case LIFE_FRUIT -> "生命果";
            case BIG_LIFE_FRUIT -> "大生命果";
            case ENERGY_STONE -> "能量石";
            case BIG_ENERGY_STONE -> "大能量石";
            case ESSENCE_GRASS -> "精华草";
            case IMMORTAL_GRASS -> "神仙草";
            case SMALL_EXP_FRUIT -> "小经验果";
            case MEDIUM_EXP_FRUIT -> "中经验果";
            case LARGE_EXP_FRUIT -> "大经验果";
        };
    }

    private String getHealingItemDescription(HealingItemType type) {
        return switch (type) {
            case LIFE_FRUIT -> "恢复50%HP";
            case BIG_LIFE_FRUIT -> "恢复100%HP";
            case ENERGY_STONE -> "恢复50%MP";
            case BIG_ENERGY_STONE -> "恢复100%MP";
            case ESSENCE_GRASS -> "恢复50%HP和50%MP";
            case IMMORTAL_GRASS -> "恢复100%HP和100%MP";
            case SMALL_EXP_FRUIT -> "直接提升1级";
            case MEDIUM_EXP_FRUIT -> "直接提升3级";
            case LARGE_EXP_FRUIT -> "直接提升5级";
        };
    }

    private String getGenderLabel(PokemonGender gender) {
        if (gender == null) {
            return PokemonGender.UNKNOWN.getDisplayName();
        }
        return gender.getDisplayName() + " " + gender.getSymbol();
    }

    private String getEggStatusLabel(EggStatus status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case READY -> "可孵化";
            case HATCHED -> "已孵化";
            default -> "孵化中";
        };
    }

    private Optional<PokemonEntity> findOwnedPokemon(Long userId, Long pokemonId) {
        return backpackRepository.findByUserIdOrderByCaughtTimeDesc(userId).stream()
                .map(BackpackEntity::getPokemon)
                .filter(Objects::nonNull)
                .filter(pokemon -> Objects.equals(pokemon.getId(), pokemonId))
                .findFirst()
                .or(() -> storageRepository.findByUserIdOrderByStoredTimeDesc(userId).stream()
                        .map(StorageEntity::getPokemon)
                        .filter(Objects::nonNull)
                        .filter(pokemon -> Objects.equals(pokemon.getId(), pokemonId))
                        .findFirst());
    }

    private int getAvailablePokemonSlots(Long userId) {
        long backpackSlots = Math.max(0, BACKPACK_MAX_SIZE - backpackRepository.countByUserId(userId));
        long storageSlots = Math.max(0, getStorageCapacity(userId) - storageRepository.countByUserId(userId));
        return (int) (backpackSlots + storageSlots);
    }

    private String placePokemonWithoutSelling(Long userId, PokemonEntity pokemon) {
        if (pokemon == null) {
            return null;
        }
        if (backpackRepository.countByUserId(userId) < BACKPACK_MAX_SIZE) {
            backpackRepository.save(new BackpackEntity(userId, pokemon));
            return "backpack";
        }
        if (storageRepository.countByUserId(userId) < getStorageCapacity(userId)) {
            storageRepository.save(new StorageEntity(userId, pokemon));
            return "storage";
        }
        return null;
    }

    private int getEggProgressByMode(BattleMode mode) {
        if (mode == null) {
            return 0;
        }
        return switch (mode) {
            case DUNGEON -> 3;
            case TRAINER -> 2;
            default -> 1;
        };
    }

    private void addBreedingProgress(Long userId, BattleMode mode) {
        int progress = getEggProgressByMode(mode);
        if (progress <= 0) {
            return;
        }
        List<PlayerEgg> eggs = playerEggRepository.findByUserIdAndStatusInOrderByCreatedAtAsc(
                userId,
                List.of(EggStatus.INCUBATING)
        );
        for (PlayerEgg egg : eggs) {
            int nextProgress = Math.min(egg.getRequiredProgress(), egg.getCurrentProgress() + progress);
            egg.setCurrentProgress(nextProgress);
            if (nextProgress >= egg.getRequiredProgress()) {
                egg.setStatus(EggStatus.READY);
            }
        }
        if (!eggs.isEmpty()) {
            playerEggRepository.saveAll(eggs);
        }
    }

    private String getAchievementName(AchievementType type) {
        return switch (type) {
            case POKEDEX_10 -> "图鉴新手";
            case POKEDEX_20 -> "图鉴大师";
            case GOLD_2000 -> "小有积蓄";
            case GOLD_5000 -> "财富积累";
            case GOLD_10000 -> "富豪训练师";
        };
    }

    private String getAchievementDescription(AchievementType type) {
        return switch (type) {
            case POKEDEX_10 -> "解锁10种精灵";
            case POKEDEX_20 -> "解锁20种精灵";
            case GOLD_2000 -> "拥有2000金币";
            case GOLD_5000 -> "拥有5000金币";
            case GOLD_10000 -> "拥有10000金币";
        };
    }

    private String getAchievementIcon(AchievementType type) {
        return switch (type) {
            case POKEDEX_10 -> "📘";
            case POKEDEX_20 -> "🏅";
            case GOLD_2000 -> "💰";
            case GOLD_5000 -> "💎";
            case GOLD_10000 -> "👑";
        };
    }

}
