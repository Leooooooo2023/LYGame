package com.example.demo.pokemon.entity;

import com.example.demo.pokemon.enums.MoveEffectType;
import com.example.demo.pokemon.enums.PokemonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "moves")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PokemonType type;

    @Column(nullable = false)
    private int power;

    @Column(nullable = false)
    private int accuracy;

    @Column(nullable = false)
    private int mpCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_type", nullable = false, length = 30)
    private MoveEffectType effectType = MoveEffectType.DAMAGE;

    @Column(name = "effect_value", nullable = false)
    private int effectValue = 0;

    @Column(name = "effect_duration", nullable = false)
    private int effectDuration = 0;

    public MoveEntity(String name, PokemonType type, int power, int accuracy, int mpCost) {
        this(name, type, power, accuracy, mpCost, MoveEffectType.DAMAGE, 0, 0);
    }

    public MoveEntity(String name, PokemonType type, int power, int accuracy, int mpCost,
                      MoveEffectType effectType, int effectValue, int effectDuration) {
        this.name = name;
        this.type = type;
        this.power = power;
        this.accuracy = accuracy;
        this.mpCost = mpCost;
        this.effectType = effectType == null ? MoveEffectType.DAMAGE : effectType;
        this.effectValue = effectValue;
        this.effectDuration = effectDuration;
    }
}
