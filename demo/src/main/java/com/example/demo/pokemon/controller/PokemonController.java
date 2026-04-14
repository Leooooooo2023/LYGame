package com.example.demo.pokemon.controller;

import com.example.demo.pokemon.entity.PokemonEntity;
import com.example.demo.pokemon.entity.PokemonMoveEntity;
import com.example.demo.pokemon.enums.PokemonType;
import com.example.demo.pokemon.service.PokemonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 宝可梦 REST API 控制器
 * 提供HTTP接口进行宝可梦的增删改查操作
 * 
 * 基础路径: /api/pokemons
 */
@RestController
@RequestMapping("/api/pokemons")
public class PokemonController {

    private final PokemonService pokemonService;

    /**
     * 构造方法注入服务
     */
    @Autowired
    public PokemonController(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    /**
     * 获取所有宝可梦
     * GET /api/pokemons
     * @return 宝可梦列表
     */
    @GetMapping
    public ResponseEntity<List<PokemonEntity>> getAllPokemons() {
        return ResponseEntity.ok(pokemonService.getAllPokemons());
    }

    /**
     * 根据ID获取宝可梦
     * GET /api/pokemons/{id}
     * @param id 宝可梦ID
     * @return 宝可梦详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPokemonById(@PathVariable Long id) {
        Optional<PokemonEntity> pokemon = pokemonService.getPokemonById(id);
        if (pokemon.isPresent()) {
            return ResponseEntity.ok(pokemon.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 根据名称获取宝可梦
     * GET /api/pokemons/name/{name}
     * @param name 宝可梦名称
     * @return 宝可梦详情
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<?> getPokemonByName(@PathVariable String name) {
        Optional<PokemonEntity> pokemon = pokemonService.getPokemonByName(name);
        if (pokemon.isPresent()) {
            return ResponseEntity.ok(pokemon.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 根据属性获取宝可梦
     * GET /api/pokemons/type/{type}
     * @param type 属性类型 (FIRE, WATER, GRASS)
     * @return 该属性的宝可梦列表
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<PokemonEntity>> getPokemonsByType(@PathVariable PokemonType type) {
        return ResponseEntity.ok(pokemonService.getPokemonsByType(type));
    }

    /**
     * 创建新宝可梦
     * POST /api/pokemons
     * @param request 包含name, type, level的请求体
     * @return 创建的宝可梦
     */
    @PostMapping
    public ResponseEntity<?> createPokemon(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            PokemonType type = PokemonType.valueOf((String) request.get("type"));
            int level = Integer.parseInt(request.get("level").toString());
            
            PokemonEntity pokemon = pokemonService.createPokemon(name, type, level);
            return ResponseEntity.ok(pokemon);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "创建失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取宝可梦的技能
     * GET /api/pokemons/{id}/moves
     * @param id 宝可梦ID
     * @return 技能列表
     */
    @GetMapping("/{id}/moves")
    public ResponseEntity<?> getPokemonMoves(@PathVariable Long id) {
        List<PokemonMoveEntity> moves = pokemonService.getPokemonMoves(id);
        return ResponseEntity.ok(moves);
    }

    /**
     * 为宝可梦学习技能
     * POST /api/pokemons/{pokemonId}/moves/{moveId}
     * @param pokemonId 宝可梦ID
     * @param moveId 技能ID
     * @return 操作结果
     */
    @PostMapping("/{pokemonId}/moves/{moveId}")
    public ResponseEntity<?> learnMove(@PathVariable Long pokemonId, @PathVariable Long moveId) {
        boolean success = pokemonService.learnMove(pokemonId, moveId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        if (success) {
            result.put("message", "学习技能成功！");
        } else {
            result.put("message", "学习技能失败（可能技能已满或ID不存在）");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 恢复宝可梦HP
     * POST /api/pokemons/{id}/heal
     * @param id 宝可梦ID
     * @return 恢复后的宝可梦
     */
    @PostMapping("/{id}/heal")
    public ResponseEntity<?> healPokemon(@PathVariable Long id) {
        Optional<PokemonEntity> pokemon = pokemonService.healPokemon(id);
        if (pokemon.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", pokemon.get().getName() + " 已完全恢复！");
            result.put("pokemon", pokemon.get());
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 删除宝可梦
     * DELETE /api/pokemons/{id}
     * @param id 宝可梦ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePokemon(@PathVariable Long id) {
        pokemonService.deletePokemon(id);
        Map<String, String> result = new HashMap<>();
        result.put("message", "宝可梦已删除");
        return ResponseEntity.ok(result);
    }
}
