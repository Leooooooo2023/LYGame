package com.example.demo.pokemon.enums;

/**
 * 宝可梦属性类型枚举（简化版）
 * 包含火、水、草三种基础属性，形成相克循环：
 * 火 -> 草 -> 水 -> 火
 * 以及普通系技能（无克制关系）
 */
public enum PokemonType {
    NORMAL("普通"),
    FIRE("火"),
    WATER("水"),
    GRASS("草");

    private final String chineseName;

    /**
     * 构造方法，设置中文名称
     * @param chineseName 属性的中文名称
     */
    PokemonType(String chineseName) {
        this.chineseName = chineseName;
    }

    /**
     * 获取属性的中文名称
     * @return 中文名称
     */
    public String getChineseName() {
        return chineseName;
    }
}
