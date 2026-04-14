package com.example.demo.pokemon.service;

import com.example.demo.pokemon.enums.PokemonType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 属性相克服务类
 * 计算宝可梦属性之间的相克关系
 * 
 * 用法示例:
 * TypeEffectivenessService service = new TypeEffectivenessService();
 * double effectiveness = service.getEffectiveness(PokemonType.WATER, PokemonType.FIRE);
 * // 返回2.0，表示水系对火系有2倍效果
 */
@Service
public class TypeEffectivenessService {

    /**
     * 属性相克表
     * Key: 攻击方属性
     * Value: Map<防御方属性, 效果倍率>
     */
    private final Map<PokemonType, Map<PokemonType, Double>> effectivenessChart;

    /**
     * 构造方法，初始化属性相克表
     */
    public TypeEffectivenessService() {
        effectivenessChart = new HashMap<>();
        initializeEffectivenessChart();
    }

    /**
     * 初始化属性相克表（简化版）
     * 火 -> 草 -> 水 -> 火 的循环相克
     * 普通系：对所有属性都是正常效果（1.0）
     */
    private void initializeEffectivenessChart() {
        // 普通系：对所有属性都是正常效果（攻击和防御都是1.0，无克制关系）
        Map<PokemonType, Double> normalEffectiveness = new HashMap<>();
        normalEffectiveness.put(PokemonType.NORMAL, 1.0);
        normalEffectiveness.put(PokemonType.FIRE, 1.0);
        normalEffectiveness.put(PokemonType.WATER, 1.0);
        normalEffectiveness.put(PokemonType.GRASS, 1.0);
        effectivenessChart.put(PokemonType.NORMAL, normalEffectiveness);
        
        // 其他属性对普通系的攻击效果也是1.0（普通系不被任何属性克制）
        // 这个在calculateEffectiveness方法中通过默认值1.0处理

        // 火系：克制草，被水克制
        Map<PokemonType, Double> fireEffectiveness = new HashMap<>();
        fireEffectiveness.put(PokemonType.NORMAL, 1.0);
        fireEffectiveness.put(PokemonType.FIRE, 1.0);   // 火打火：正常效果
        fireEffectiveness.put(PokemonType.WATER, 0.5);  // 火打水：效果不好
        fireEffectiveness.put(PokemonType.GRASS, 2.0);  // 火打草：效果拔群
        effectivenessChart.put(PokemonType.FIRE, fireEffectiveness);

        // 水系：克制火，被草克制
        Map<PokemonType, Double> waterEffectiveness = new HashMap<>();
        waterEffectiveness.put(PokemonType.NORMAL, 1.0);
        waterEffectiveness.put(PokemonType.FIRE, 2.0);  // 水打火：效果拔群
        waterEffectiveness.put(PokemonType.WATER, 1.0); // 水打水：正常效果
        waterEffectiveness.put(PokemonType.GRASS, 0.5); // 水打草：效果不好
        effectivenessChart.put(PokemonType.WATER, waterEffectiveness);

        // 草系：克制水，被火克制
        Map<PokemonType, Double> grassEffectiveness = new HashMap<>();
        grassEffectiveness.put(PokemonType.NORMAL, 1.0);
        grassEffectiveness.put(PokemonType.FIRE, 0.5);  // 草打火：效果不好
        grassEffectiveness.put(PokemonType.WATER, 2.0); // 草打水：效果拔群
        grassEffectiveness.put(PokemonType.GRASS, 1.0); // 草打草：正常效果
        effectivenessChart.put(PokemonType.GRASS, grassEffectiveness);
    }

    /**
     * 获取属性相克效果
     * 
     * @param attackType 攻击方属性
     * @param defenseType 防御方属性
     * @return 效果倍率（0.0, 0.5, 1.0, 2.0）
     *         0.0 = 无效，0.5 = 效果不好，1.0 = 正常，2.0 = 效果拔群
     */
    public double getEffectiveness(PokemonType attackType, PokemonType defenseType) {
        Map<PokemonType, Double> attackEffectiveness = effectivenessChart.get(attackType);
        if (attackEffectiveness != null && attackEffectiveness.containsKey(defenseType)) {
            return attackEffectiveness.get(defenseType);
        }
        return 1.0; // 默认正常效果
    }

    /**
     * 计算对多属性宝可梦的效果倍率
     * 
     * @param attackType 攻击方属性
     * @param defenseTypes 防御方属性列表
     * @return 总效果倍率
     */
    public double calculateEffectiveness(PokemonType attackType, java.util.List<PokemonType> defenseTypes) {
        double totalEffectiveness = 1.0;
        for (PokemonType defenseType : defenseTypes) {
            totalEffectiveness *= getEffectiveness(attackType, defenseType);
        }
        return totalEffectiveness;
    }

    /**
     * 获取效果描述
     * 
     * @param effectiveness 效果倍率
     * @return 描述文字
     */
    public String getEffectivenessDescription(double effectiveness) {
        if (effectiveness == 0.0) {
            return "没有效果...";
        } else if (effectiveness < 1.0) {
            return "效果不太好...";
        } else if (effectiveness > 1.0) {
            return "效果拔群！";
        } else {
            return "";
        }
    }
}
