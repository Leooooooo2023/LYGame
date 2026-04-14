package com.example.demo.pokemon.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum PokemonTalentType {

    BRAVE("勇猛", "基本攻击属性+5", 5, 0, 0, 0, 0, "普通", "#7f8c8d", 18),
    TENACIOUS("顽强", "基本防御属性+5", 0, 5, 0, 0, 0, "普通", "#7f8c8d", 18),
    SWIFT("神速", "基本速度属性+5", 0, 0, 5, 0, 0, "普通", "#7f8c8d", 18),
    LONGEVITY("长生", "基本HP属性+20", 0, 0, 0, 20, 0, "稀有", "#3498db", 14),
    WISDOM("智慧", "基本MP属性+20", 0, 0, 0, 0, 20, "稀有", "#3498db", 14),
    ALL_ROUNDER("全能高手", "基本攻击、防御、速度属性+5", 5, 5, 5, 0, 0, "史诗", "#9b59b6", 8),
    DIVINE_GRACE("神的恩泽", "基本HP、MP属性+20", 0, 0, 0, 20, 20, "传说", "#e67e22", 4);

    private final String displayName;
    private final String description;
    private final int attackBonus;
    private final int defenseBonus;
    private final int speedBonus;
    private final int hpBonus;
    private final int mpBonus;
    private final String rarityLabel;
    private final String color;
    private final int weight;

    PokemonTalentType(String displayName, String description, int attackBonus, int defenseBonus, int speedBonus,
                      int hpBonus, int mpBonus, String rarityLabel, String color, int weight) {
        this.displayName = displayName;
        this.description = description;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.speedBonus = speedBonus;
        this.hpBonus = hpBonus;
        this.mpBonus = mpBonus;
        this.rarityLabel = rarityLabel;
        this.color = color;
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    public int getSpeedBonus() {
        return speedBonus;
    }

    public int getHpBonus() {
        return hpBonus;
    }

    public int getMpBonus() {
        return mpBonus;
    }

    public String getRarityLabel() {
        return rarityLabel;
    }

    public String getColor() {
        return color;
    }

    public int getWeight() {
        return weight;
    }

    public static Optional<PokemonTalentType> fromName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(value -> value.name().toLowerCase(Locale.ROOT).equals(normalized)
                        || value.displayName.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    public static List<PokemonTalentType> all() {
        return Collections.unmodifiableList(Arrays.asList(values()));
    }
}