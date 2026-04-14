package com.example.demo.pokemon.entity;

import com.example.demo.pokemon.enums.PokemonTalentType;
import com.example.demo.pokemon.enums.PokemonGender;
import com.example.demo.pokemon.enums.PokemonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 宝可梦数据库实体类
 * 用于在MySQL数据库中存储宝可梦信息
 * 
 * 对应数据库表: pokemons
 */
@Entity
@Table(name = "pokemons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonEntity {

    public static final int MAX_LEVEL = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PokemonType type;

    @Column(nullable = false)
    private int rarity = 1;

    @Column(nullable = false)
    private int level;

    @Column(name = "max_hp", nullable = false)
    private int maxHp;

    @Column(name = "current_hp", nullable = false)
    private int currentHp;

    @Column(nullable = false)
    private int attack;

    @Column(nullable = false)
    private int defense;

    @Column(name = "special_attack", nullable = false)
    private int specialAttack;

    @Column(name = "special_defense", nullable = false)
    private int specialDefense;

    @Column(nullable = false)
    private int speed;

    @Column(name = "is_fainted", nullable = false)
    private boolean fainted = false;

    @Column(nullable = false)
    private int experience = 0;

    @Column(name = "max_mp", nullable = false)
    private int maxMp;

    @Column(name = "current_mp", nullable = false)
    private int currentMp;

    @Column(name = "talents", length = 255)
    private String talents;

    @Column(name = "talent_applied", nullable = false)
    private boolean talentApplied = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PokemonGender gender = PokemonGender.UNKNOWN;

    public PokemonEntity(String name, PokemonType type, int level) {
        this.name = name;
        this.type = type;
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        initializeStats();
    }

    private void initializeStats() {
        this.maxHp = 50 + level * 3;
        this.currentHp = maxHp;
        this.maxMp = 30 + level * 2;
        this.currentMp = maxMp;
        this.attack = 40 + level * 2;
        this.defense = 40 + level * 2;
        this.specialAttack = 40 + level * 2;
        this.specialDefense = 40 + level * 2;
        this.speed = 40 + level * 2;
    }

    public List<PokemonTalentType> getTalentTypeList() {
        if (talents == null || talents.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(talents.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(PokemonTalentType::fromName)
                .flatMap(optional -> optional.stream())
                .collect(Collectors.toList());
    }

    public List<PokemonTalentInfo> getTalentInfos() {
        return getTalentTypeList().stream()
                .map(talent -> new PokemonTalentInfo(talent.name(), talent.getDisplayName(), talent.getDescription(), talent.getRarityLabel(), talent.getColor()))
                .collect(Collectors.toList());
    }

    public void setTalentTypeList(List<PokemonTalentType> talentTypes) {
        if (talentTypes == null || talentTypes.isEmpty()) {
            this.talents = null;
            this.talentApplied = false;
            return;
        }
        this.talents = talentTypes.stream()
                .distinct()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        this.talentApplied = false;
    }

    public void applyTalentBonusesIfNeeded() {
        if (talentApplied) {
            return;
        }
        List<PokemonTalentType> talentTypes = getTalentTypeList();
        if (talentTypes.isEmpty()) {
            this.talentApplied = true;
            return;
        }
        for (PokemonTalentType talentType : talentTypes) {
            applyTalentBonus(talentType);
        }
        this.talentApplied = true;
    }

    private void applyTalentBonus(PokemonTalentType talentType) {
        this.attack += talentType.getAttackBonus();
        this.defense += talentType.getDefenseBonus();
        this.speed += talentType.getSpeedBonus();
        this.maxHp += talentType.getHpBonus();
        this.currentHp += talentType.getHpBonus();
        this.maxMp += talentType.getMpBonus();
        this.currentMp += talentType.getMpBonus();
        if (this.currentHp > this.maxHp) {
            this.currentHp = this.maxHp;
        }
        if (this.currentMp > this.maxMp) {
            this.currentMp = this.maxMp;
        }
    }

    public boolean consumeMp(int amount) {
        if (this.currentMp < amount) {
            return false;
        }
        this.currentMp -= amount;
        return true;
    }

    public void restoreMp(int amount) {
        this.currentMp += amount;
        if (this.currentMp > this.maxMp) {
            this.currentMp = this.maxMp;
        }
    }

    public void takeDamage(int damage) {
        this.currentHp -= damage;
        if (this.currentHp <= 0) {
            this.currentHp = 0;
            this.fainted = true;
        }
    }

    public void heal(int amount) {
        this.currentHp += amount;
        if (this.currentHp > this.maxHp) {
            this.currentHp = this.maxHp;
        }
    }

    public void fullRestore() {
        this.currentHp = this.maxHp;
        this.currentMp = this.maxMp;
        this.fainted = false;
    }

    public ExperienceGainResult gainExperience(int exp) {
        int actualExp = Math.max(0, exp);
        int oldLevel = Math.max(1, this.level);
        int oldExperience = Math.max(0, this.experience);
        if (oldLevel >= MAX_LEVEL || actualExp == 0) {
            this.level = Math.min(MAX_LEVEL, oldLevel);
            this.experience = Math.min(oldExperience, getTotalExperienceForLevel(MAX_LEVEL));
            return new ExperienceGainResult(0, this.level, this.level, this.experience, getExpToNextLevel(), false);
        }

        this.experience = oldExperience + actualExp;
        boolean leveledUp = false;
        while (this.level < MAX_LEVEL && this.experience >= getTotalExperienceForLevel(this.level + 1)) {
            this.level++;
            applyLevelGrowth(this.level);
            leveledUp = true;
        }

        if (this.level >= MAX_LEVEL) {
            this.level = MAX_LEVEL;
            this.experience = Math.min(this.experience, getTotalExperienceForLevel(MAX_LEVEL));
        }

        if (leveledUp) {
            fullRestore();
        }

        return new ExperienceGainResult(actualExp, oldLevel, this.level, this.experience, getExpToNextLevel(), leveledUp);
    }

    public void scaleToLevel(int targetLevel) {
        int clampedLevel = Math.max(1, Math.min(MAX_LEVEL, targetLevel));
        if (this.level <= 0) {
            this.level = 1;
        }
        while (this.level < clampedLevel) {
            this.level++;
            applyLevelGrowth(this.level);
        }
        if (this.level > clampedLevel) {
            this.level = clampedLevel;
        }
        this.experience = getTotalExperienceForLevel(this.level);
        fullRestore();
    }

    public int getExpToNextLevel() {
        if (this.level >= MAX_LEVEL) {
            return 0;
        }
        return Math.max(0, getTotalExperienceForLevel(this.level + 1) - this.experience);
    }

    public int getExpProgressInCurrentLevel() {
        return Math.max(0, this.experience - getTotalExperienceForLevel(this.level));
    }

    public int getExpRequiredForCurrentLevel() {
        if (this.level >= MAX_LEVEL) {
            return 0;
        }
        return getTotalExperienceForLevel(this.level + 1) - getTotalExperienceForLevel(this.level);
    }

    private void applyLevelGrowth(int newLevel) {
        this.maxHp += 5 + this.rarity + (newLevel % 5 == 0 ? 2 : 0);
        this.maxMp += 2 + (this.rarity >= 3 ? 1 : 0) + (newLevel % 10 == 0 ? 1 : 0);

        int highestStatIndex = getHighestBattleStatIndex();
        int secondStatIndex = getSecondHighestBattleStatIndex(highestStatIndex);

        this.attack += 1;
        this.defense += 1;
        this.specialAttack += 1;
        this.specialDefense += 1;
        this.speed += 1;

        applyExtraStatGrowth(highestStatIndex, 1);
        if (newLevel % 3 == 0) {
            applyExtraStatGrowth(secondStatIndex, 1);
        }
    }

    private int getHighestBattleStatIndex() {
        int[] stats = {attack, defense, specialAttack, specialDefense, speed};
        int bestIndex = 0;
        for (int i = 1; i < stats.length; i++) {
            if (stats[i] > stats[bestIndex]) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private int getSecondHighestBattleStatIndex(int highestIndex) {
        int[] stats = {attack, defense, specialAttack, specialDefense, speed};
        int secondIndex = highestIndex == 0 ? 1 : 0;
        for (int i = 0; i < stats.length; i++) {
            if (i == highestIndex) {
                continue;
            }
            if (secondIndex == highestIndex || stats[i] > stats[secondIndex]) {
                secondIndex = i;
            }
        }
        return secondIndex;
    }

    private void applyExtraStatGrowth(int statIndex, int amount) {
        switch (statIndex) {
            case 0 -> this.attack += amount;
            case 1 -> this.defense += amount;
            case 2 -> this.specialAttack += amount;
            case 3 -> this.specialDefense += amount;
            case 4 -> this.speed += amount;
            default -> {
            }
        }
    }

    public static int getTotalExperienceForLevel(int level) {
        int clampedLevel = Math.max(1, Math.min(MAX_LEVEL, level));
        int total = 0;
        for (int currentLevel = 1; currentLevel < clampedLevel; currentLevel++) {
            total += getRequiredExperienceForLevel(currentLevel);
        }
        return total;
    }

    public static int getRequiredExperienceForLevel(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        int stage = Math.max(0, level - 1);
        return 30 + stage * 8 + (stage / 5) * 6;
    }

    @Data
    @AllArgsConstructor
    public static class ExperienceGainResult {
        private int gainedExp;
        private int oldLevel;
        private int newLevel;
        private int totalExperience;
        private int expToNextLevel;
        private boolean leveledUp;
    }
}
