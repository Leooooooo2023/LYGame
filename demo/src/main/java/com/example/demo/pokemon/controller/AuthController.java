package com.example.demo.pokemon.controller;

import com.example.demo.pokemon.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 单机版入口控制器
 * 保留极少量兼容接口，统一改为本地单机模式。
 */
@Controller
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/game";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/game";
    }

    @GetMapping("/api/auth/current")
    @ResponseBody
    public ResponseEntity<?> getCurrentUser() {
        Map<String, Object> result = new HashMap<>();
        var user = authService.ensureLocalUser();
        result.put("loggedIn", true);
        result.put("role", "LOCAL");
        result.put("username", user.getUsername());
        result.put("gold", user.getGold());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<?> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "单机版无需登录，已保持本地存档");
        return ResponseEntity.ok(result);
    }
}
