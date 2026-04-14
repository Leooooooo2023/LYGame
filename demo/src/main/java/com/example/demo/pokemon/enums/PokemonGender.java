package com.example.demo.pokemon.enums;

public enum PokemonGender {
    MALE("雄性", "♂"),
    FEMALE("雌性", "♀"),
    UNKNOWN("未知", "?");

    private final String displayName;
    private final String symbol;

    PokemonGender(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }
}
