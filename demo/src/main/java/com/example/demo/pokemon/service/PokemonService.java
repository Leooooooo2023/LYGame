package com.example.demo.pokemon.service;

import jakarta.annotation.PostConstruct;
import com.example.demo.pokemon.entity.MoveEntity;
import com.example.demo.pokemon.entity.PokemonEntity;
import com.example.demo.pokemon.entity.PokemonMoveEntity;
import com.example.demo.pokemon.enums.MoveEffectType;
import com.example.demo.pokemon.enums.PokemonGender;
import com.example.demo.pokemon.enums.PokemonTalentType;
import com.example.demo.pokemon.enums.PokemonType;
import com.example.demo.pokemon.repository.BackpackRepository;
import com.example.demo.pokemon.repository.MoveRepository;
import com.example.demo.pokemon.repository.PokedexRepository;
import com.example.demo.pokemon.repository.PokemonMoveRepository;
import com.example.demo.pokemon.repository.PokemonRepository;
import com.example.demo.pokemon.repository.StorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * 宝可梦业务逻辑层
 * 处理宝可梦的创建、查询、更新等业务逻辑
 */
@Service
public class PokemonService {

    private static final List<String> SPECIAL_POKEMON_NAMES = Arrays.asList("战神驼", "火凤凰", "潮汐海皇", "灵光鹿");
    private static final Map<String, EvolutionRule> EVOLUTION_RULES = Map.ofEntries(
            Map.entry("小火狐", new EvolutionRule("烈焰虎", 16)),
            Map.entry("烈焰虎", new EvolutionRule("熔岩兽", 32)),
            Map.entry("小水滴", new EvolutionRule("激流鲨", 16)),
            Map.entry("激流鲨", new EvolutionRule("深海巨鲸", 32)),
            Map.entry("嫩芽苗", new EvolutionRule("叶伞蛙", 16)),
            Map.entry("叶伞蛙", new EvolutionRule("远古树精", 32)),
            Map.entry("迅影鼠", new EvolutionRule("月灵兔", 20)),
            Map.entry("岩甲熊", new EvolutionRule("晶石巨人", 24))
    );

    private final PokemonRepository pokemonRepository;
    private final MoveRepository moveRepository;
    private final PokemonMoveRepository pokemonMoveRepository;
    private final BackpackRepository backpackRepository;
    private final PokedexRepository pokedexRepository;
    private final StorageRepository storageRepository;
    private final Random random = new Random();

    /**
     * 构造方法注入依赖
     */
    @Autowired
    public PokemonService(PokemonRepository pokemonRepository, 
                         MoveRepository moveRepository,
                         PokemonMoveRepository pokemonMoveRepository,
                         BackpackRepository backpackRepository,
                         PokedexRepository pokedexRepository,
                         StorageRepository storageRepository) {
        this.pokemonRepository = pokemonRepository;
        this.moveRepository = moveRepository;
        this.pokemonMoveRepository = pokemonMoveRepository;
        this.backpackRepository = backpackRepository;
        this.pokedexRepository = pokedexRepository;
        this.storageRepository = storageRepository;
    }

    /**
     * 创建新宝可梦
     * @param name 名称
     * @param type 属性
     * @param level 等级
     * @return 创建的宝可梦
     */
    @Transactional
    public PokemonEntity createPokemon(String name, PokemonType type, int level) {
        PokemonEntity pokemon = new PokemonEntity(name, type, level);
        return pokemonRepository.save(pokemon);
    }

    @PostConstruct
    @Transactional
    public void ensureBasicMoveForAllPokemons() {
        createOrUpdateMove("撞击", PokemonType.NORMAL, 30, 100, 0, true);
        pokemonRepository.findAll().forEach(pokemon -> ensureMoveByName(pokemon.getId(), "撞击"));
        assignMissingOwnedPokemonGenders();
    }

    private static class EvolutionRule {
        private final String targetName;
        private final int requiredLevel;

        private EvolutionRule(String targetName, int requiredLevel) {
            this.targetName = targetName;
            this.requiredLevel = requiredLevel;
        }
    }

    private List<PokemonTalentType> generateRandomTalents() {
        int count = random.nextInt(3);
        if (count == 0) {
            return new ArrayList<>();
        }

        List<PokemonTalentType> selected = new ArrayList<>();
        List<PokemonTalentType> pool = new ArrayList<>(PokemonTalentType.all());
        while (selected.size() < count && !pool.isEmpty()) {
            PokemonTalentType talent = drawWeightedTalent(pool);
            selected.add(talent);
            pool.remove(talent);
        }
        return selected;
    }

    private PokemonTalentType drawWeightedTalent(List<PokemonTalentType> pool) {
        int totalWeight = pool.stream().mapToInt(PokemonTalentType::getWeight).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        int cumulative = 0;
        for (PokemonTalentType talentType : pool) {
            cumulative += talentType.getWeight();
            if (roll < cumulative) {
                return talentType;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private PokemonEntity createClonedPokemon(PokemonEntity template, boolean randomizeTalents) {
        return createClonedPokemon(template, randomizeTalents, null, null);
    }

    private PokemonEntity createClonedPokemon(PokemonEntity template,
                                              boolean randomizeTalents,
                                              PokemonGender forcedGender,
                                              List<PokemonTalentType> fixedTalents) {
        PokemonEntity cloned = new PokemonEntity();
        cloned.setName(template.getName());
        cloned.setType(template.getType());
        cloned.setLevel(template.getLevel());
        cloned.setMaxHp(template.getMaxHp());
        cloned.setCurrentHp(template.getMaxHp());
        cloned.setAttack(template.getAttack());
        cloned.setDefense(template.getDefense());
        cloned.setSpecialAttack(template.getSpecialAttack());
        cloned.setSpecialDefense(template.getSpecialDefense());
        cloned.setSpeed(template.getSpeed());
        cloned.setMaxMp(template.getMaxMp());
        cloned.setCurrentMp(template.getMaxMp());
        cloned.setRarity(template.getRarity());
        cloned.setFainted(false);
        cloned.setExperience(0);
        cloned.setTalents(null);
        cloned.setTalentApplied(false);
        cloned.setGender(forcedGender == null ? drawRandomGender() : forcedGender);
        if (fixedTalents != null) {
            cloned.setTalentTypeList(limitTalents(fixedTalents));
            cloned.applyTalentBonusesIfNeeded();
            cloned.fullRestore();
        } else if (randomizeTalents) {
            cloned.setTalentTypeList(generateRandomTalents());
            cloned.applyTalentBonusesIfNeeded();
            cloned.fullRestore();
        }
        cloned = pokemonRepository.save(cloned);

        List<PokemonMoveEntity> moves = pokemonMoveRepository.findByPokemonId(template.getId());
        for (PokemonMoveEntity pm : moves) {
            PokemonMoveEntity newPm = new PokemonMoveEntity(cloned, pm.getMove());
            pokemonMoveRepository.save(newPm);
        }
        ensureMoveByName(cloned.getId(), "撞击");
        return cloned;
    }

    private List<PokemonTalentType> limitTalents(List<PokemonTalentType> talents) {
        if (talents == null || talents.isEmpty()) {
            return new ArrayList<>();
        }
        return talents.stream().distinct().limit(2).toList();
    }

    public PokemonGender drawRandomGender() {
        return random.nextBoolean() ? PokemonGender.MALE : PokemonGender.FEMALE;
    }

    @Transactional
    public void assignMissingOwnedPokemonGenders() {
        Set<Long> processed = new HashSet<>();
        backpackRepository.findAll().forEach(item -> {
            PokemonEntity pokemon = item.getPokemon();
            if (pokemon != null && pokemon.getId() != null && processed.add(pokemon.getId())
                    && (pokemon.getGender() == null || pokemon.getGender() == PokemonGender.UNKNOWN)) {
                pokemon.setGender(drawRandomGender());
                pokemonRepository.save(pokemon);
            }
        });
        storageRepository.findAll().forEach(item -> {
            PokemonEntity pokemon = item.getPokemon();
            if (pokemon != null && pokemon.getId() != null && processed.add(pokemon.getId())
                    && (pokemon.getGender() == null || pokemon.getGender() == PokemonGender.UNKNOWN)) {
                pokemon.setGender(drawRandomGender());
                pokemonRepository.save(pokemon);
            }
        });
    }

    public boolean isSpecialPokemon(String name) {
        return SPECIAL_POKEMON_NAMES.contains(name);
    }

    public boolean canBreed(PokemonEntity first, PokemonEntity second) {
        if (first == null || second == null || first.getId() == null || second.getId() == null) {
            return false;
        }
        if (first.getId().equals(second.getId())) {
            return false;
        }
        if (!first.getName().equals(second.getName())) {
            return false;
        }
        if (isSpecialPokemon(first.getName())) {
            return false;
        }
        return first.getGender() != null
                && second.getGender() != null
                && first.getGender() != PokemonGender.UNKNOWN
                && second.getGender() != PokemonGender.UNKNOWN
                && first.getGender() != second.getGender();
    }

    public Optional<PokemonEntity> getTemplatePokemonByName(String name) {
        Set<Long> ownedIds = new HashSet<>();
        backpackRepository.findAll().forEach(item -> {
            if (item.getPokemon() != null && item.getPokemon().getId() != null) {
                ownedIds.add(item.getPokemon().getId());
            }
        });
        storageRepository.findAll().forEach(item -> {
            if (item.getPokemon() != null && item.getPokemon().getId() != null) {
                ownedIds.add(item.getPokemon().getId());
            }
        });
        return pokemonRepository.findAll().stream()
                .filter(pokemon -> pokemon.getName().equals(name))
                .filter(pokemon -> pokemon.getId() != null && !ownedIds.contains(pokemon.getId()))
                .sorted(Comparator.comparing(PokemonEntity::getId))
                .findFirst()
                .or(() -> pokemonRepository.findByName(name));
    }

    @Transactional
    public PokemonEntity createOffspringPokemon(String pokemonName, List<PokemonTalentType> inheritedTalents) {
        PokemonEntity template = getTemplatePokemonByName(pokemonName)
                .orElseThrow(() -> new IllegalArgumentException("未找到孵化模板精灵: " + pokemonName));
        return createClonedPokemon(template, false, drawRandomGender(), limitTalents(inheritedTalents));
    }

    public List<PokemonTalentType> rollInheritedTalents(PokemonEntity father, PokemonEntity mother) {
        List<PokemonTalentType> pool = new ArrayList<>();
        if (father != null) {
            pool.addAll(father.getTalentTypeList());
        }
        if (mother != null) {
            pool.addAll(mother.getTalentTypeList());
        }
        pool = pool.stream().distinct().toList();
        if (pool.isEmpty()) {
            return new ArrayList<>();
        }
        List<PokemonTalentType> shuffled = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled, random);
        int targetCount = Math.min(Math.min(2, shuffled.size()), shuffled.size() == 1 ? 1 : 1 + random.nextInt(2));
        return shuffled.stream().limit(targetCount).toList();
    }

    public Optional<String> getEvolutionTargetName(PokemonEntity pokemon) {
        if (pokemon == null) {
            return Optional.empty();
        }
        EvolutionRule rule = EVOLUTION_RULES.get(pokemon.getName());
        if (rule == null || pokemon.getLevel() < rule.requiredLevel) {
            return Optional.empty();
        }
        return Optional.of(rule.targetName);
    }

    public Optional<Integer> getEvolutionRequiredLevel(String pokemonName) {
        EvolutionRule rule = EVOLUTION_RULES.get(pokemonName);
        return rule == null ? Optional.empty() : Optional.of(rule.requiredLevel);
    }

    public Optional<String> getEvolutionPreviewTargetName(String pokemonName) {
        EvolutionRule rule = EVOLUTION_RULES.get(pokemonName);
        return rule == null ? Optional.empty() : Optional.of(rule.targetName);
    }

    @Transactional
    public PokemonEntity evolvePokemon(PokemonEntity pokemon) {
        String targetName = getEvolutionTargetName(pokemon)
                .orElseThrow(() -> new IllegalStateException("当前精灵未满足进化条件"));
        PokemonEntity template = getTemplatePokemonByName(targetName)
                .orElseThrow(() -> new IllegalStateException("未找到进化目标模板"));

        int currentLevel = pokemon.getLevel();
        int currentExperience = pokemon.getExperience();
        PokemonGender currentGender = pokemon.getGender() == null ? drawRandomGender() : pokemon.getGender();
        List<PokemonTalentType> talents = new ArrayList<>(pokemon.getTalentTypeList());

        pokemon.setName(template.getName());
        pokemon.setType(template.getType());
        pokemon.setRarity(template.getRarity());
        pokemon.setLevel(template.getLevel());
        pokemon.setMaxHp(template.getMaxHp());
        pokemon.setCurrentHp(template.getCurrentHp());
        pokemon.setAttack(template.getAttack());
        pokemon.setDefense(template.getDefense());
        pokemon.setSpecialAttack(template.getSpecialAttack());
        pokemon.setSpecialDefense(template.getSpecialDefense());
        pokemon.setSpeed(template.getSpeed());
        pokemon.setMaxMp(template.getMaxMp());
        pokemon.setCurrentMp(template.getCurrentMp());
        pokemon.setExperience(0);
        pokemon.setFainted(false);
        pokemon.setGender(currentGender);
        pokemon.setTalents(null);
        pokemon.setTalentApplied(false);
        pokemon.scaleToLevel(currentLevel);
        pokemon.setExperience(Math.max(PokemonEntity.getTotalExperienceForLevel(currentLevel), currentExperience));
        pokemon.setTalentTypeList(talents);
        pokemon.applyTalentBonusesIfNeeded();
        pokemon.fullRestore();
        pokemon = pokemonRepository.save(pokemon);
        refreshPokemonMoveSet(pokemon);
        return pokemon;
    }

    @Transactional
    public void refreshPokemonMoveSet(PokemonEntity pokemon) {
        if (pokemon == null || pokemon.getId() == null) {
            return;
        }
        List<String> moveNames = buildRecommendedMoveSet(pokemon);
        if (moveNames.isEmpty()) {
            return;
        }
        List<MoveEntity> moves = moveNames.stream()
                .map(moveRepository::findByName)
                .flatMap(Optional::stream)
                .limit(4)
                .toList();
        if (moves.isEmpty()) {
            return;
        }
        pokemonMoveRepository.deleteByPokemonId(pokemon.getId());
        for (MoveEntity move : moves) {
            pokemonMoveRepository.save(new PokemonMoveEntity(pokemon, move));
        }
        ensureMoveByName(pokemon.getId(), "撞击");
    }

    /**
     * 获取所有精灵（只返回新版精灵：宝贝龙、水手企鹅、叶伞蛙）
     * @return 精灵列表
     */
    public List<PokemonEntity> getAllPokemons() {
        // 返回所有精灵（包括初始精灵和野外精灵）
        return pokemonRepository.findAll();
    }

    /**
     * 获取初始精灵列表（只包含3个初始精灵）
     * @return 初始精灵列表
     */
    public List<PokemonEntity> getStarterPokemons() {
        List<PokemonEntity> pokemons = new ArrayList<>();
        pokemonRepository.findByName("宝贝龙").ifPresent(pokemons::add);
        pokemonRepository.findByName("水手企鹅").ifPresent(pokemons::add);
        pokemonRepository.findByName("叶伞蛙").ifPresent(pokemons::add);
        return pokemons;
    }

    /**
     * 根据ID获取宝可梦
     * @param id 宝可梦ID
     * @return 宝可梦（可能为空）
     */
    public Optional<PokemonEntity> getPokemonById(Long id) {
        return pokemonRepository.findById(id);
    }

    /**
     * 根据名称获取宝可梦
     * @param name 宝可梦名称
     * @return 宝可梦（可能为空）
     */
    public Optional<PokemonEntity> getPokemonByName(String name) {
        return pokemonRepository.findByName(name);
    }

    /**
     * 根据属性获取宝可梦
     * @param type 属性类型
     * @return 宝可梦列表
     */
    public List<PokemonEntity> getPokemonsByType(PokemonType type) {
        return pokemonRepository.findByType(type);
    }

    /**
     * 为宝可梦学习技能
     * @param pokemonId 宝可梦ID
     * @param moveId 技能ID
     * @return 是否成功
     */
    @Transactional
    public boolean learnMove(Long pokemonId, Long moveId) {
        Optional<PokemonEntity> pokemonOpt = pokemonRepository.findById(pokemonId);
        Optional<MoveEntity> moveOpt = moveRepository.findById(moveId);

        if (pokemonOpt.isPresent() && moveOpt.isPresent()) {
            // 检查是否已有4个技能
            List<PokemonMoveEntity> existingMoves = pokemonMoveRepository.findByPokemonId(pokemonId);
            if (existingMoves.size() >= 4) {
                return false; // 技能已满
            }

            PokemonMoveEntity pokemonMove = new PokemonMoveEntity(
                pokemonOpt.get(), moveOpt.get()
            );
            pokemonMoveRepository.save(pokemonMove);
            return true;
        }
        return false;
    }

    /**
     * 获取宝可梦的所有技能
     * @param pokemonId 宝可梦ID
     * @return 技能关联列表
     */
    public List<PokemonMoveEntity> getPokemonMoves(Long pokemonId) {
        return pokemonMoveRepository.findByPokemonId(pokemonId);
    }
    
    /**
     * 根据宝可梦名称获取技能
     * @param pokemonName 宝可梦名称
     * @return 技能关联列表
     */
    public List<PokemonMoveEntity> getPokemonMovesByName(String pokemonName) {
        Optional<PokemonEntity> pokemon = pokemonRepository.findByName(pokemonName);
        return pokemon.map(p -> pokemonMoveRepository.findByPokemonId(p.getId())).orElse(new ArrayList<>());
    }

    /**
     * 恢复宝可梦HP和MP
     * @param id 宝可梦ID
     * @return 恢复后的宝可梦
     */
    @Transactional
    public Optional<PokemonEntity> healPokemon(Long id) {
        Optional<PokemonEntity> pokemonOpt = pokemonRepository.findById(id);
        if (pokemonOpt.isPresent()) {
            PokemonEntity pokemon = pokemonOpt.get();
            pokemon.fullRestore();
            // 恢复MP
            pokemon.setCurrentMp(pokemon.getMaxMp());
            
            return Optional.of(pokemonRepository.save(pokemon));
        }
        return Optional.empty();
    }

    /**
     * 删除宝可梦
     * @param id 宝可梦ID
     */
    @Transactional
    public void deletePokemon(Long id) {
        // 先删除技能关联
        pokemonMoveRepository.deleteByPokemonId(id);
        // 再删除宝可梦
        pokemonRepository.deleteById(id);
    }

    /**
     * 初始化基础数据
     * 创建初始宝可梦和技能
     */
    @Transactional
    public void initializeData() {
        // 清空所有旧数据，重新初始化
        // 注意：先清空有外键关联的表（按依赖顺序）

        // 创建普通系技能（所有精灵都可以学习）
        createOrUpdateMove("撞击", PokemonType.NORMAL, 30, 100, 0, true);      // 低威力，不消耗MP
        createOrUpdateMove("猛撞", PokemonType.NORMAL, 60, 90, 0, true);       // 中等威力，不消耗MP
        createOrUpdateMove("破坏光线", PokemonType.NORMAL, 120, 90, 20, true); // 高威力，消耗MP
        // 战神驼专属技能 - 全面碾压普通技能
        createOrUpdateMove("圣战踢踏", PokemonType.NORMAL, 150, 95, 30, true); // 超高威力，强力单攻
        createOrUpdateMove("驼峰地震", PokemonType.NORMAL, 130, 100, 25, true); // 高威力，高命中
        
        // 创建火系技能 - MP消耗递增
        createOrUpdateMove("火花", PokemonType.FIRE, 40, 100, 8, false);
        createOrUpdateMove("火焰牙", PokemonType.FIRE, 65, 95, 12, false);
        createOrUpdateMove("喷射火焰", PokemonType.FIRE, 90, 100, 18, false);
        createOrUpdateMove("大字爆", PokemonType.FIRE, 110, 85, 25, false);
        // 火凤凰专属技能
        createOrUpdateMove("神圣之火", PokemonType.FIRE, 140, 95, 28, false);
        
        // 创建水系技能 - MP消耗递增
        createOrUpdateMove("水枪", PokemonType.WATER, 40, 100, 8, false);
        createOrUpdateMove("泡沫光线", PokemonType.WATER, 65, 100, 12, false);
        createOrUpdateMove("水流尾", PokemonType.WATER, 90, 90, 18, false);
        createOrUpdateMove("水炮", PokemonType.WATER, 110, 80, 25, false);
        // 潮汐海皇专属技能
        createOrUpdateMove("海啸冲击", PokemonType.WATER, 135, 90, 26, false);
        
        // 创建草系技能 - MP消耗递增
        createOrUpdateMove("藤鞭", PokemonType.GRASS, 45, 100, 8, false);
        createOrUpdateMove("飞叶快刀", PokemonType.GRASS, 55, 95, 10, false);
        createOrUpdateMove("能量球", PokemonType.GRASS, 90, 100, 18, false);
        createOrUpdateMove("日光束", PokemonType.GRASS, 120, 100, 28, false);
        // 灵光鹿专属技能
        createOrUpdateMove("灵光绽放", PokemonType.GRASS, 130, 95, 24, false);

        // 创建辅助系技能
        createOrUpdateMove("治愈之息", PokemonType.NORMAL, 0, 100, 10, MoveEffectType.HEAL_SELF, 35, 0, true);
        createOrUpdateMove("铁壁", PokemonType.NORMAL, 0, 100, 8, MoveEffectType.BUFF_DEFENSE, 8, 3, true);
        createOrUpdateMove("战意提升", PokemonType.NORMAL, 0, 100, 8, MoveEffectType.BUFF_ATTACK, 6, 3, true);
        createOrUpdateMove("迅捷姿态", PokemonType.NORMAL, 0, 100, 8, MoveEffectType.BUFF_SPEED, 6, 3, true);
        createOrUpdateMove("威吓", PokemonType.NORMAL, 0, 95, 8, MoveEffectType.DEBUFF_ATTACK, 6, 3, true);
        createOrUpdateMove("破甲战吼", PokemonType.NORMAL, 0, 95, 10, MoveEffectType.DEBUFF_DEFENSE, 8, 3, true);
        createOrUpdateMove("迟缓诅咒", PokemonType.NORMAL, 0, 95, 8, MoveEffectType.DEBUFF_SPEED, 6, 3, true);
        createOrUpdateMove("烈焰鼓舞", PokemonType.FIRE, 0, 100, 10, MoveEffectType.BUFF_ATTACK, 8, 3, true);
        createOrUpdateMove("水幕守护", PokemonType.WATER, 0, 100, 10, MoveEffectType.BUFF_DEFENSE, 10, 3, true);
        createOrUpdateMove("生命绽放", PokemonType.GRASS, 0, 100, 12, MoveEffectType.HEAL_SELF, 45, 0, true);

        // 创建初始精灵 - 全部为1级，根据稀有度平衡属性
        // 属性总值（HP+ATK+DEF+SPD）参考区间：1星≈76，2星≈93-97，3星≈110-118，4星≈131-138，5星≈177

        // 火系 - 宝贝龙（2星）：速攻型
        if (pokemonRepository.count() > 0) {
            return;
        }

        PokemonEntity charmander = createPokemon("宝贝龙", PokemonType.FIRE, 1);
        charmander.setRarity(2);
        charmander.setMaxHp(58);
        charmander.setCurrentHp(58);
        charmander.setAttack(13);
        charmander.setDefense(9);
        charmander.setSpeed(14);  // HP+A+D+S=94
        charmander.setMaxMp(36);
        charmander.setCurrentMp(36);
        charmander = pokemonRepository.save(charmander);
        learnMoveByName(charmander.getId(), "撞击");
        learnMoveByName(charmander.getId(), "火花");

        // 水系 - 水手企鹅（2星）：防御型
        PokemonEntity squirtle = createPokemon("水手企鹅", PokemonType.WATER, 1);
        squirtle.setRarity(2);
        squirtle.setMaxHp(64);
        squirtle.setCurrentHp(64);
        squirtle.setAttack(11);
        squirtle.setDefense(14);
        squirtle.setSpeed(8);  // HP+A+D+S=97
        squirtle.setMaxMp(38);
        squirtle.setCurrentMp(38);
        squirtle = pokemonRepository.save(squirtle);
        learnMoveByName(squirtle.getId(), "撞击");
        learnMoveByName(squirtle.getId(), "水枪");

        // 草系 - 叶伞蛙（2星）：平衡型
        PokemonEntity bulbasaur = createPokemon("叶伞蛙", PokemonType.GRASS, 1);
        bulbasaur.setRarity(2);
        bulbasaur.setMaxHp(62);
        bulbasaur.setCurrentHp(62);
        bulbasaur.setAttack(12);
        bulbasaur.setDefense(12);
        bulbasaur.setSpeed(11);  // HP+A+D+S=97
        bulbasaur.setMaxMp(37);
        bulbasaur.setCurrentMp(37);
        bulbasaur = pokemonRepository.save(bulbasaur);
        learnMoveByName(bulbasaur.getId(), "撞击");
        learnMoveByName(bulbasaur.getId(), "藤鞭");

        // ========== 野外精灵（只能在战斗中遇到）==========

        // 火系野外精灵
        // 小火狐 - 1星，速攻型
        PokemonEntity fox = createPokemon("小火狐", PokemonType.FIRE, 1);
        fox.setRarity(1);
        fox.setMaxHp(47);
        fox.setCurrentHp(47);
        fox.setAttack(10);
        fox.setDefense(7);
        fox.setSpeed(13);  // HP+A+D+S=77
        fox.setMaxMp(30);
        fox.setCurrentMp(30);
        fox = pokemonRepository.save(fox);
        learnMoveByName(fox.getId(), "撞击");
        learnMoveByName(fox.getId(), "火花");

        // 烈焰虎 - 3星，速攻型
        PokemonEntity tiger = createPokemon("烈焰虎", PokemonType.FIRE, 1);
        tiger.setRarity(3);
        tiger.setMaxHp(68);
        tiger.setCurrentHp(68);
        tiger.setAttack(15);
        tiger.setDefense(11);
        tiger.setSpeed(15);  // HP+A+D+S=109
        tiger.setMaxMp(44);
        tiger.setCurrentMp(44);
        tiger = pokemonRepository.save(tiger);
        learnMoveByName(tiger.getId(), "撞击");
        learnMoveByName(tiger.getId(), "火花");
        learnMoveByName(tiger.getId(), "火焰牙");

        // 熔岩兽 - 4星，攻击型
        PokemonEntity lava = createPokemon("熔岩兽", PokemonType.FIRE, 1);
        lava.setRarity(4);
        lava.setMaxHp(86);
        lava.setCurrentHp(86);
        lava.setAttack(19);
        lava.setDefense(14);
        lava.setSpeed(12);  // HP+A+D+S=131
        lava.setMaxMp(55);
        lava.setCurrentMp(55);
        lava = pokemonRepository.save(lava);
        learnMoveByName(lava.getId(), "撞击");
        learnMoveByName(lava.getId(), "火焰牙");
        learnMoveByName(lava.getId(), "喷射火焰");

        // 水系野外精灵
        // 小水滴 - 1星，防御型
        PokemonEntity droplet = createPokemon("小水滴", PokemonType.WATER, 1);
        droplet.setRarity(1);
        droplet.setMaxHp(50);
        droplet.setCurrentHp(50);
        droplet.setAttack(8);
        droplet.setDefense(11);
        droplet.setSpeed(7);  // HP+A+D+S=76
        droplet.setMaxMp(32);
        droplet.setCurrentMp(32);
        droplet = pokemonRepository.save(droplet);
        learnMoveByName(droplet.getId(), "撞击");
        learnMoveByName(droplet.getId(), "水枪");

        // 激流鲨 - 3星，速攻型
        PokemonEntity shark = createPokemon("激流鲨", PokemonType.WATER, 1);
        shark.setRarity(3);
        shark.setMaxHp(68);
        shark.setCurrentHp(68);
        shark.setAttack(16);
        shark.setDefense(12);
        shark.setSpeed(14);  // HP+A+D+S=110
        shark.setMaxMp(44);
        shark.setCurrentMp(44);
        shark = pokemonRepository.save(shark);
        learnMoveByName(shark.getId(), "撞击");
        learnMoveByName(shark.getId(), "水枪");
        learnMoveByName(shark.getId(), "泡沫光线");

        // 深海巨鲸 - 4星，超坦型
        PokemonEntity whale = createPokemon("深海巨鲸", PokemonType.WATER, 1);
        whale.setRarity(4);
        whale.setMaxHp(96);
        whale.setCurrentHp(96);
        whale.setAttack(15);
        whale.setDefense(20);
        whale.setSpeed(7);  // HP+A+D+S=138
        whale.setMaxMp(58);
        whale.setCurrentMp(58);
        whale = pokemonRepository.save(whale);
        learnMoveByName(whale.getId(), "撞击");
        learnMoveByName(whale.getId(), "泡沫光线");
        learnMoveByName(whale.getId(), "水流尾");

        // 草系野外精灵
        // 嫩芽苗 - 1星，平衡型
        PokemonEntity sprout = createPokemon("嫩芽苗", PokemonType.GRASS, 1);
        sprout.setRarity(1);
        sprout.setMaxHp(48);
        sprout.setCurrentHp(48);
        sprout.setAttack(9);
        sprout.setDefense(9);
        sprout.setSpeed(10);  // HP+A+D+S=76
        sprout.setMaxMp(30);
        sprout.setCurrentMp(30);
        sprout = pokemonRepository.save(sprout);
        learnMoveByName(sprout.getId(), "撞击");
        learnMoveByName(sprout.getId(), "藤鞭");

        // 毒蘑菇 - 3星，防御型
        PokemonEntity mushroom = createPokemon("毒蘑菇", PokemonType.GRASS, 1);
        mushroom.setRarity(3);
        mushroom.setMaxHp(72);
        mushroom.setCurrentHp(72);
        mushroom.setAttack(13);
        mushroom.setDefense(16);
        mushroom.setSpeed(10);  // HP+A+D+S=111
        mushroom.setMaxMp(46);
        mushroom.setCurrentMp(46);
        mushroom = pokemonRepository.save(mushroom);
        learnMoveByName(mushroom.getId(), "撞击");
        learnMoveByName(mushroom.getId(), "藤鞭");
        learnMoveByName(mushroom.getId(), "飞叶快刀");

        // 远古树精 - 4星，坦攻型
        PokemonEntity treant = createPokemon("远古树精", PokemonType.GRASS, 1);
        treant.setRarity(4);
        treant.setMaxHp(90);
        treant.setCurrentHp(90);
        treant.setAttack(16);
        treant.setDefense(18);
        treant.setSpeed(9);  // HP+A+D+S=133
        treant.setMaxMp(56);
        treant.setCurrentMp(56);
        treant = pokemonRepository.save(treant);
        learnMoveByName(treant.getId(), "撞击");
        learnMoveByName(treant.getId(), "飞叶快刀");
        learnMoveByName(treant.getId(), "能量球");

        // ========== 战神驼（5星，仅可通过特等奖抽奖获得）==========
        PokemonEntity godCamel = createPokemon("战神驼", PokemonType.NORMAL, 1);
        godCamel.setRarity(5);
        godCamel.setMaxHp(115);
        godCamel.setCurrentHp(115);
        godCamel.setAttack(23);
        godCamel.setDefense(21);
        godCamel.setSpeed(18);  // HP+A+D+S=177
        godCamel.setMaxMp(80);
        godCamel.setCurrentMp(80);
        godCamel = pokemonRepository.save(godCamel);
        learnMoveByName(godCamel.getId(), "撞击");
        learnMoveByName(godCamel.getId(), "猛撞");
        learnMoveByName(godCamel.getId(), "圣战踢踏");
        learnMoveByName(godCamel.getId(), "驼峰地震");

        // ========== 副本专属精灵（5星，仅可通过副本挑战获得）==========
        
        // 火凤凰 - 5星，火系，副本专属
        PokemonEntity phoenix = createPokemon("火凤凰", PokemonType.FIRE, 1);
        phoenix.setRarity(5);
        phoenix.setMaxHp(110);
        phoenix.setCurrentHp(110);
        phoenix.setAttack(24);
        phoenix.setDefense(19);
        phoenix.setSpeed(20);  // HP+A+D+S=173
        phoenix.setMaxMp(75);
        phoenix.setCurrentMp(75);
        phoenix = pokemonRepository.save(phoenix);
        learnMoveByName(phoenix.getId(), "撞击");
        learnMoveByName(phoenix.getId(), "火焰牙");
        learnMoveByName(phoenix.getId(), "喷射火焰");
        learnMoveByName(phoenix.getId(), "神圣之火");
        
        // 潮汐海皇 - 5星，水系，副本专属
        PokemonEntity seaEmperor = createPokemon("潮汐海皇", PokemonType.WATER, 1);
        seaEmperor.setRarity(5);
        seaEmperor.setMaxHp(118);
        seaEmperor.setCurrentHp(118);
        seaEmperor.setAttack(22);
        seaEmperor.setDefense(20);
        seaEmperor.setSpeed(16);  // HP+A+D+S=176
        seaEmperor.setMaxMp(78);
        seaEmperor.setCurrentMp(78);
        seaEmperor = pokemonRepository.save(seaEmperor);
        learnMoveByName(seaEmperor.getId(), "撞击");
        learnMoveByName(seaEmperor.getId(), "泡沫光线");
        learnMoveByName(seaEmperor.getId(), "水流尾");
        learnMoveByName(seaEmperor.getId(), "海啸冲击");
        
        // 灵光鹿 - 5星，草系，副本专属
        PokemonEntity spiritDeer = createPokemon("灵光鹿", PokemonType.GRASS, 1);
        spiritDeer.setRarity(5);
        spiritDeer.setMaxHp(105);
        spiritDeer.setCurrentHp(105);
        spiritDeer.setAttack(21);
        spiritDeer.setDefense(22);
        spiritDeer.setSpeed(19);  // HP+A+D+S=167
        spiritDeer.setMaxMp(72);
        spiritDeer.setCurrentMp(72);
        spiritDeer = pokemonRepository.save(spiritDeer);
        learnMoveByName(spiritDeer.getId(), "撞击");
        learnMoveByName(spiritDeer.getId(), "飞叶快刀");
        learnMoveByName(spiritDeer.getId(), "能量球");
        learnMoveByName(spiritDeer.getId(), "灵光绽放");

        // ========== 普通系精灵（野外遇到）==========

        // 迅影鼠 - 1星，极速型
        PokemonEntity squirrel = createPokemon("迅影鼠", PokemonType.NORMAL, 1);
        squirrel.setRarity(1);
        squirrel.setMaxHp(47);
        squirrel.setCurrentHp(47);
        squirrel.setAttack(9);
        squirrel.setDefense(7);
        squirrel.setSpeed(14);  // HP+A+D+S=77
        squirrel.setMaxMp(30);
        squirrel.setCurrentMp(30);
        squirrel = pokemonRepository.save(squirrel);
        learnMoveByName(squirrel.getId(), "撞击");
        learnMoveByName(squirrel.getId(), "猛撞");
        learnMoveByName(squirrel.getId(), "破坏光线");

        // 月灵兔 - 2星，均衡型
        PokemonEntity rabbit = createPokemon("月灵兔", PokemonType.NORMAL, 1);
        rabbit.setRarity(2);
        rabbit.setMaxHp(58);
        rabbit.setCurrentHp(58);
        rabbit.setAttack(12);
        rabbit.setDefense(11);
        rabbit.setSpeed(12);  // HP+A+D+S=93
        rabbit.setMaxMp(36);
        rabbit.setCurrentMp(36);
        rabbit = pokemonRepository.save(rabbit);
        learnMoveByName(rabbit.getId(), "撞击");
        learnMoveByName(rabbit.getId(), "猛撞");
        learnMoveByName(rabbit.getId(), "破坏光线");

        // 岩甲熊 - 3星，高攻坦型
        PokemonEntity bear = createPokemon("岩甲熊", PokemonType.NORMAL, 1);
        bear.setRarity(3);
        bear.setMaxHp(78);
        bear.setCurrentHp(78);
        bear.setAttack(17);
        bear.setDefense(14);
        bear.setSpeed(9);  // HP+A+D+S=118
        bear.setMaxMp(46);
        bear.setCurrentMp(46);
        bear = pokemonRepository.save(bear);
        learnMoveByName(bear.getId(), "撞击");
        learnMoveByName(bear.getId(), "猛撞");
        learnMoveByName(bear.getId(), "破坏光线");

        // 晶石巨人 - 4星，防御型
        PokemonEntity golem = createPokemon("晶石巨人", PokemonType.NORMAL, 1);
        golem.setRarity(4);
        golem.setMaxHp(93);
        golem.setCurrentHp(93);
        golem.setAttack(15);
        golem.setDefense(21);
        golem.setSpeed(7);  // HP+A+D+S=136
        golem.setMaxMp(58);
        golem.setCurrentMp(58);
        golem = pokemonRepository.save(golem);
        learnMoveByName(golem.getId(), "撞击");
        learnMoveByName(golem.getId(), "猛撞");
        learnMoveByName(golem.getId(), "破坏光线");
    }

    /**
     * 获取所有可通过抽奖赠予玩家的精灵（排除战神驼和副本专属精灵）
     * 从数据库中随机取一只精灵，克隆后赠予玩家（不影响原始精灵数据）
     * @return 随机普通精灵（非特殊精灵），没有可用精灵时返回空
     */
    @Transactional
    public Optional<PokemonEntity> getRandomNonSpecialPokemonForLottery() {
        List<String> specialPokemonNames = Arrays.asList("战神驼", "火凤凰", "潮汐海皇", "灵光鹿");
        List<PokemonEntity> candidates = pokemonRepository.findAll().stream()
                .filter(p -> !specialPokemonNames.contains(p.getName()))
                .collect(java.util.stream.Collectors.toList());
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        PokemonEntity template = candidates.get(new Random().nextInt(candidates.size()));
        return Optional.of(createClonedPokemon(template, true));
    }

    /**
     * 克隆一只战神驼精灵赠给玩家（特等奖专用）
     * 始终满血满MP，保留全部技能
     * @return 克隆出的战神驼
     */
    @Transactional
    public Optional<PokemonEntity> cloneGodCamelForLottery() {
        Optional<PokemonEntity> templateOpt = pokemonRepository.findByName("战神驼");
        if (templateOpt.isEmpty()) {
            return Optional.empty();
        }
        PokemonEntity template = templateOpt.get();
        return Optional.of(createClonedPokemon(template, true));
    }

    /**
     * 克隆精灵用于战斗（副本挑战中的敌方精灵）
     * 满血满MP，保留全部技能
     * @param template 模板精灵
     * @return 克隆出的精灵
     */
    @Transactional
    public PokemonEntity clonePokemonForBattle(PokemonEntity template) {
        return createClonedPokemon(template, true);
    }

    /**
     * 克隆精灵作为奖励赠给玩家（副本通关奖励）
     * 满血满MP，保留全部技能
     * @param template 模板精灵
     * @return 克隆出的精灵
     */
    @Transactional
    public PokemonEntity clonePokemonForReward(PokemonEntity template) {
        return createClonedPokemon(template, true);
    }

    /**
     * 创建或更新技能
     * @param name 技能名称
     * @param type 技能属性
     * @param power 威力
     * @param accuracy 命中率
     * @param mpCost MP消耗
     * @param updateIfExists 如果存在是否更新
     */
    private void createOrUpdateMove(String name, PokemonType type, int power, int accuracy, int mpCost, boolean updateIfExists) {
        createOrUpdateMove(name, type, power, accuracy, mpCost, MoveEffectType.DAMAGE, 0, 0, updateIfExists);
    }

    private void createOrUpdateMove(String name,
                                    PokemonType type,
                                    int power,
                                    int accuracy,
                                    int mpCost,
                                    MoveEffectType effectType,
                                    int effectValue,
                                    int effectDuration,
                                    boolean updateIfExists) {
        Optional<MoveEntity> existingMove = moveRepository.findByName(name);
        if (existingMove.isPresent() && updateIfExists) {
            MoveEntity move = existingMove.get();
            move.setType(type);
            move.setPower(power);
            move.setAccuracy(accuracy);
            move.setMpCost(mpCost);
            move.setEffectType(effectType);
            move.setEffectValue(effectValue);
            move.setEffectDuration(effectDuration);
            moveRepository.save(move);
        } else if (existingMove.isEmpty()) {
            moveRepository.save(new MoveEntity(name, type, power, accuracy, mpCost, effectType, effectValue, effectDuration));
        }
    }

    @Transactional
    protected void normalizePokemonMoveSets() {
        pokemonRepository.findAll().forEach(pokemon -> {
            List<String> moveNames = buildRecommendedMoveSet(pokemon);
            if (moveNames.isEmpty()) {
                return;
            }
            List<MoveEntity> moves = moveNames.stream()
                    .map(moveRepository::findByName)
                    .flatMap(Optional::stream)
                    .limit(4)
                    .toList();
            boolean hasZeroMp = moves.stream().anyMatch(move -> move.getMpCost() == 0);
            boolean hasMpDamage = moves.stream().anyMatch(move -> move.getMpCost() > 0 && (move.getEffectType() == null || move.getEffectType() == MoveEffectType.DAMAGE) && move.getPower() > 0);
            if (moves.isEmpty() || !hasZeroMp || !hasMpDamage) {
                return;
            }
            pokemonMoveRepository.deleteByPokemonId(pokemon.getId());
            for (MoveEntity move : moves) {
                pokemonMoveRepository.save(new PokemonMoveEntity(pokemon, move));
            }
        });
    }

    private List<String> buildRecommendedMoveSet(PokemonEntity pokemon) {
        List<String> moves = new ArrayList<>();
        moves.add("撞击");
        switch (pokemon.getType()) {
            case FIRE -> {
                moves.add(pokemon.getRarity() >= 4 ? "喷射火焰" : "火花");
                if (pokemon.getRarity() >= 5) {
                    moves.add("神圣之火");
                } else if (pokemon.getRarity() >= 3) {
                    moves.add("火焰牙");
                }
                moves.add(pokemon.getRarity() >= 3 ? "烈焰鼓舞" : "迅捷姿态");
            }
            case WATER -> {
                moves.add(pokemon.getRarity() >= 4 ? "水流尾" : "水枪");
                if (pokemon.getRarity() >= 5) {
                    moves.add("海啸冲击");
                } else if (pokemon.getRarity() >= 3) {
                    moves.add("泡沫光线");
                }
                moves.add("水幕守护");
            }
            case GRASS -> {
                moves.add(pokemon.getRarity() >= 4 ? "能量球" : "藤鞭");
                if (pokemon.getRarity() >= 5) {
                    moves.add("灵光绽放");
                } else if (pokemon.getRarity() >= 3) {
                    moves.add("飞叶快刀");
                }
                moves.add(pokemon.getRarity() >= 4 ? "生命绽放" : "治愈之息");
            }
            default -> {
                moves.add(pokemon.getRarity() >= 4 ? "破坏光线" : "猛撞");
                if (pokemon.getRarity() >= 5) {
                    moves.add("圣战踢踏");
                    moves.add("战意提升");
                } else if (pokemon.getRarity() >= 4) {
                    moves.add("破甲战吼");
                    moves.add("铁壁");
                } else if (pokemon.getRarity() >= 3) {
                    moves.add("铁壁");
                    moves.add("破坏光线");
                } else {
                    moves.add("迅捷姿态");
                    moves.add("治愈之息");
                }
            }
        }
        return moves.stream().distinct().limit(4).toList();
    }

    /**
     * ????????
     */
    private void learnMoveByName(Long pokemonId, String moveName) {
        ensureMoveByName(pokemonId, moveName);
    }

    private void ensureMoveByName(Long pokemonId, String moveName) {
        moveRepository.findByName(moveName).ifPresent(move -> {
            PokemonEntity pokemon = pokemonRepository.findById(pokemonId).orElse(null);
            if (pokemon != null) {
                boolean alreadyLearned = pokemonMoveRepository.findByPokemonId(pokemonId).stream()
                        .anyMatch(existing -> existing.getMove() != null
                                && existing.getMove().getId() != null
                                && existing.getMove().getId().equals(move.getId()));
                if (alreadyLearned) {
                    return;
                }
                PokemonMoveEntity pokemonMove = new PokemonMoveEntity(pokemon, move);
                pokemonMoveRepository.save(pokemonMove);
            }
        });
    }

    /**
     * 初始化图鉴描述数据
     * 应用启动后写入每只精灵的简介/故事，丰富图鉴内容展示
     * 幂等操作：已有描述则不覆盖
     */
    @Transactional
    public void initPokedexDescriptions() {
        if (pokemonRepository.count() >= 0) {
            return;
        }
        Map<String, String> descriptions = new java.util.LinkedHashMap<>();
        descriptions.put("宝贝龙", "从火山蛋中孵化出的火系幼龙，虽然体型小巧但已展现出惊人的火焰天赋。传说它喷出的第一口火焰能照亮整个洞穴，是训练师们最喜爱的初始伙伴之一。");
        descriptions.put("小火狐", "生活在火山近处的活泼火系精灵，火焰尾巴凌烈燃烧，传说火焰熄灭时代表它情绪低落。反应极快，又狡黠又勇猎。");
        descriptions.put("烈焰虎", "小火狐进化后的霸丽形态，背部的火焰燃烧起来黑烟滚滚。扫勉一眼即能判断敌人实力强弱，战斗期间从不过度热血。");
        descriptions.put("熔岩兽", "混合了燕岩与渔滚利爪的火系神兽，肯测无火海过，足迹遥遮，最爱把有趣的对手挺结了之后拼命一烧。");
        descriptions.put("小水滴", "孕生于清澈山泉的可爱水系精灵，身上常常聚集木晨露珠。性格温柔内敛，才能稳定，不怒就是局面就秆了一八。");
        descriptions.put("激流鲨", "深海里的冷血猎手，齿弓如刀。永远独行，却接受任何挑战。水手形黑影在海底顾盼珍品的故事，是每个渔夫最恐惧的四字。");
        descriptions.put("深海巨鲸", "盘测于深海沟壑的巳笛鳌鱼。传说每隔亿年才会浮出水面一次，山峦般巨大的身影引发海啸，却患极永远不会伤害无辜之物。");
        descriptions.put("水手企鹅", "纱子所块却吓到许多小鱼的唱歌家。2000公尺滂水里照样能弹出流场理顺的水球，游泳速度生生打败水下没且身水流动的小鱼。");
        descriptions.put("嫩芽苗", "初生的幼小草系精灵，头顶常常携载一片鲜嫩的小叶片。对阳光情有独特感应，日出时歌唱的节奏能让周围植物加速生长。");
        descriptions.put("叶伞蛙", "公认的吴下大免斗建门面子，拿之没有什么用只有它建定道义士最清楚。背上量身计效不错，出山战之前永远先到刀山上擀一巡。");
        descriptions.put("毒蘑菇", "弓身于阴暗潮湿森林里的草系精灵，斗笠上的斑点积踏着天然试验的烙印。小动物进居它附近的地盘变得赐沃，却仅局部超出标了。");
        descriptions.put("远古树精", "由百年大树得道化身而成，树签就是不够粗的树史。深夜独自在森林巴同一团战屡。周身市免于时间的扎记，古婿而自道。");
        descriptions.put("迅影鼠", "超高速的普通系精灵，踪迹局女就像广姐的气概。投影技术崴点，闲不了了淙进人组进行小测试提前勘路，具有东方神第六感。");
        descriptions.put("月灵兔", "月光下结成的秘感歌为味道最香的普通系精灵，长耳是夜空最好的天线。对情绪其实拥有极强的感应力，却小天把这种能力敞示出来。");
        descriptions.put("岩甲熊", "天生的养矻古达人，个头大却心地细腿，掌握一片石山山巃的山大王。老家在才石缝双，搬马来回一趣味多了一个坦荡的升十分。");
        descriptions.put("晶石巨人", "由千年山眼陶炉凝一个固个罗汉的平静了上奖利，创破击济与手腥一直正的震撼人心。被它拳每拳一拳的眼睛中，内心不由就在山峦上背弹出弹子一样射门进马其中两点狡惧霸举。");
        descriptions.put("战神驼", "超爆级广域中原驼神，历战百佐从未败下阵。其娓娓的驼峰被传说内藏捻级第五维力山的天地灵气，自过巨龙以身稽纳天比赛中，就要指着她了。谱传而来的天地传奇漫操后才离开。谱天动地弌的传奇精灵，只可来自最幸运的抽奖承接。");
        descriptions.put("火凤凰", "传说中的火焰神鸟，羽翼如烈焰般燃烧，翱翔于天际时如同第二轮太阳。只有最勇敢的训练师才能在副本挑战中见到它的真容，击败它的人将获得它的认可。");
        descriptions.put("潮汐海皇", "深海之中的绝对王者，掌控着海洋的潮汐之力。它的一声咆哮能引发海啸，一次摆尾能掀起巨浪。只有通关副本挑战的强者才能让它俯首称臣。");
        descriptions.put("灵光鹿", "森林深处的神秘精灵，身上散发着治愈万物的灵光。它所到之处枯木逢春，是生命的象征。只有在副本挑战中证明自己的人，才能获得它的追随。");

        descriptions.forEach((name, desc) -> {
            com.example.demo.pokemon.entity.PokedexEntity entry =
                    null;
            if (entry != null && (entry.getDescription() == null || entry.getDescription().isBlank())) {
                entry.setDescription(desc);
                pokedexRepository.save(entry);
            } else if (entry == null) {
                pokemonRepository.findAll().stream()
                        .filter(p -> p.getName().equals(name))
                        .findFirst()
                        .ifPresent(p -> {
                            com.example.demo.pokemon.entity.PokedexEntity newEntry =
                                    new com.example.demo.pokemon.entity.PokedexEntity();
                            newEntry.setUserId(1L);
                            newEntry.setPokemonName(p.getName());
                            newEntry.setPokemonType(p.getType().name());
                            newEntry.setDescription(desc);
                            pokedexRepository.save(newEntry);
                        });
            }
        });
    }

}
