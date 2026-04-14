package com.example.demo.pokemon.enums;

/**
 * 精灵球类型枚举
 */
public enum PokeBallType {
    /**
     * 初级精灵球 - 50金币，基础概率10%
     */
    BASIC(50, 10, "初级精灵球"),
    
    /**
     * 中级精灵球 - 100金币，基础概率20%
     */
    MEDIUM(100, 20, "中级精灵球"),
    
    /**
     * 高级精灵球 - 200金币，基础概率30%
     */
    ADVANCED(200, 30, "高级精灵球"),
    
    /**
     * 大师球 - 500金币，基础概率100%
     */
    MASTER(500, 100, "大师球");
    
    private final int price;
    private final int baseCatchRate;
    private final String displayName;
    
    PokeBallType(int price, int baseCatchRate, String displayName) {
        this.price = price;
        this.baseCatchRate = baseCatchRate;
        this.displayName = displayName;
    }
    
    public int getPrice() {
        return price;
    }
    
    public int getBaseCatchRate() {
        return baseCatchRate;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
