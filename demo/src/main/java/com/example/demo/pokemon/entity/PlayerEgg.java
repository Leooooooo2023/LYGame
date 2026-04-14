package com.example.demo.pokemon.entity;

import com.example.demo.pokemon.enums.EggStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_eggs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerEgg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "father_pokemon_id", nullable = false)
    private PokemonEntity fatherPokemon;

    @ManyToOne
    @JoinColumn(name = "mother_pokemon_id", nullable = false)
    private PokemonEntity motherPokemon;

    @Column(name = "egg_name", nullable = false, length = 50)
    private String eggName;

    @Column(name = "target_pokemon_name", nullable = false, length = 50)
    private String targetPokemonName;

    @Column(name = "current_progress", nullable = false)
    private int currentProgress = 0;

    @Column(name = "required_progress", nullable = false)
    private int requiredProgress = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EggStatus status = EggStatus.INCUBATING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "hatched_at")
    private LocalDateTime hatchedAt;

    public PlayerEgg(Long userId,
                     PokemonEntity fatherPokemon,
                     PokemonEntity motherPokemon,
                     String eggName,
                     String targetPokemonName,
                     int requiredProgress) {
        this.userId = userId;
        this.fatherPokemon = fatherPokemon;
        this.motherPokemon = motherPokemon;
        this.eggName = eggName;
        this.targetPokemonName = targetPokemonName;
        this.requiredProgress = requiredProgress;
        this.currentProgress = 0;
        this.status = EggStatus.INCUBATING;
        this.createdAt = LocalDateTime.now();
    }
}
