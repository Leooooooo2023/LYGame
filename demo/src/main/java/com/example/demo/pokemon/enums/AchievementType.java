package com.example.demo.pokemon.enums;

/**
 * 成就类型枚举
 */
public enum AchievementType {
    /**
     * 图鉴解锁10种精灵
     */
    POKEDEX_10("图鉴新手", "解锁10种精灵", 500, "💎"),
    
    /**
     * 图鉴解锁20种精灵
     */
    POKEDEX_20("图鉴大师", "解锁20种精灵", 1000, "🏆"),
    
    /**
     * 拥有2000金币
     */
    GOLD_2000("小有积蓄", "拥有2000金币", 500, "💰"),
    
    /**
     * 拥有5000金币
     */
    GOLD_5000("财富积累", "拥有5000金币", 1000, "💎"),
    
    /**
     * 拥有10000金币
     */
    GOLD_10000("富豪训练师", "拥有10000金币", 0, "👑") {
        @Override
        public int getMasterBallReward() {
            return 10;
        }
    };
    
    private final String displayName;
    private final String description;
    private final int goldReward;
    private final String icon;
    
    AchievementType(String displayName, String description, int goldReward, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.goldReward = goldReward;
        this.icon = icon;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getGoldReward() {
        return goldReward;
    }
    
    public String getIcon() {
        return icon;
    }
    
    /**
     * 获取大师球奖励数量（仅GOLD_10000有）
     */
    public int getMasterBallReward() {
        return 0;
    }
}
