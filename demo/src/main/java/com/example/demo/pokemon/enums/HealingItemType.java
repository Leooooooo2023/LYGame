package com.example.demo.pokemon.enums;

/**
 * 恢复类/成长类道具类型枚举
 */
public enum HealingItemType {
    /**
     * 生命果 - 恢复50%HP，价格100金币
     */
    LIFE_FRUIT(100, 50, 0, 0, "生命果", "🍎"),

    /**
     * 大生命果 - 恢复100%HP，价格200金币
     */
    BIG_LIFE_FRUIT(200, 100, 0, 0, "大生命果", "🍏"),

    /**
     * 能量石 - 恢复50%MP，价格100金币
     */
    ENERGY_STONE(100, 0, 50, 0, "能量石", "💎"),

    /**
     * 大能量石 - 恢复100%MP，价格200金币
     */
    BIG_ENERGY_STONE(200, 0, 100, 0, "大能量石", "💠"),

    /**
     * 精华草 - 恢复50%HP和50%MP，价格200金币
     */
    ESSENCE_GRASS(200, 50, 50, 0, "精华草", "🌿"),

    /**
     * 神仙草 - 恢复100%HP和100%MP，价格300金币
     */
    IMMORTAL_GRASS(300, 100, 100, 0, "神仙草", "🌱"),

    /**
     * 小经验果 - 直接提升1级，价格100金币
     */
    SMALL_EXP_FRUIT(100, 0, 0, 1, "小经验果", "🍊"),

    /**
     * 中经验果 - 直接提升3级，价格200金币
     */
    MEDIUM_EXP_FRUIT(200, 0, 0, 3, "中经验果", "🍑"),

    /**
     * 大经验果 - 直接提升5级，价格300金币
     */
    LARGE_EXP_FRUIT(300, 0, 0, 5, "大经验果", "🍍");

    private final int price;
    private final int hpPercent;
    private final int mpPercent;
    private final int levelGain;
    private final String displayName;
    private final String icon;

    HealingItemType(int price, int hpPercent, int mpPercent, int levelGain, String displayName, String icon) {
        this.price = price;
        this.hpPercent = hpPercent;
        this.mpPercent = mpPercent;
        this.levelGain = levelGain;
        this.displayName = displayName;
        this.icon = icon;
    }

    public int getPrice() {
        return price;
    }

    public int getHpPercent() {
        return hpPercent;
    }

    public int getMpPercent() {
        return mpPercent;
    }

    public int getLevelGain() {
        return levelGain;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isExperienceFruit() {
        return levelGain > 0;
    }

    public boolean isBattleUsable() {
        return !isExperienceFruit();
    }

    /**
     * 获取道具描述
     */
    public String getDescription() {
        if (isExperienceFruit()) {
            return String.format("直接提升%d级", levelGain);
        }
        if (hpPercent > 0 && mpPercent > 0) {
            return String.format("恢复%d%%HP和%d%%MP", hpPercent, mpPercent);
        } else if (hpPercent > 0) {
            return String.format("恢复%d%%HP", hpPercent);
        } else if (mpPercent > 0) {
            return String.format("恢复%d%%MP", mpPercent);
        }
        return "";
    }
}
