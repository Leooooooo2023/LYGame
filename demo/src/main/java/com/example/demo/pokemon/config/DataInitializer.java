package com.example.demo.pokemon.config;

import com.example.demo.pokemon.entity.User;
import com.example.demo.pokemon.service.AuthService;
import com.example.demo.pokemon.service.PlayerHealingItemService;
import com.example.demo.pokemon.service.PlayerInventoryService;
import com.example.demo.pokemon.service.PokemonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 应用启动时自动初始化基础数据（精灵和技能）以及本地单机存档。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final PokemonService pokemonService;
    private final PlayerInventoryService playerInventoryService;
    private final PlayerHealingItemService playerHealingItemService;
    private final AuthService authService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataInitializer(PokemonService pokemonService, PlayerInventoryService playerInventoryService,
                           PlayerHealingItemService playerHealingItemService, AuthService authService,
                           JdbcTemplate jdbcTemplate) {
        this.pokemonService = pokemonService;
        this.playerInventoryService = playerInventoryService;
        this.playerHealingItemService = playerHealingItemService;
        this.authService = authService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        cleanupLegacyAuthSchema();

        logger.info("Initializing local single-player pokemon data...");
        User user = authService.ensureLocalUser();
        playerInventoryService.ensureDefaultInventory(user.getId());
        playerHealingItemService.ensureDefaultInventory(user.getId());
        pokemonService.initializeData();
        logger.info("Initializing pokedex descriptions...");
        pokemonService.initPokedexDescriptions();
        logger.info("Local single-player data initialization completed!");
    }

    /**
     * 清理已经废弃的登录/管理员数据库结构。
     */
    private void cleanupLegacyAuthSchema() {
        try {
            logger.info("正在清理旧版登录/管理员表结构...");
            jdbcTemplate.execute("DROP TABLE IF EXISTS admins");
            // 先删除 password 和 last_login 列（如果存在）
            jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS password");
            jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS last_login");
            jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS enabled");
            logger.info("旧版登录/管理员表结构清理完成");
        } catch (Exception ex) {
            logger.warn("清理旧版登录结构时出现非致命问题：{}", ex.getMessage());
        }
    }
}
