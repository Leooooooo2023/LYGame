package com.example.demo.pokemon.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PokemonTalentInfo {
    private String key;
    private String name;
    private String description;
    private String rarity;
    private String color;
}