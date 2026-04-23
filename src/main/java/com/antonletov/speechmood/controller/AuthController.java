package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.LoginPayload;
import com.antonletov.speechmood.dto.RegisterPayload;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/register/")
    public ResponseEntity<?> register(@RequestBody RegisterPayload payload) {
        System.out.println("Регистрация: " + payload.getUser().getUsername());
        return ResponseEntity.ok(Map.of("message", "Успешно зарегистрировано!"));
    }

    @PostMapping("/login/")
    public ResponseEntity<?> login(@RequestBody LoginPayload payload) {
        // пока пусть здесь будет заглушка, to do разобраться с Spring Security
        return ResponseEntity.ok(Map.of(
                "access", "dummy-access-token",
                "refresh", "dummy-refresh-token"
        ));
    }

    @PostMapping("/token/refresh/")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(Map.of("access", "new-dummy-access-token"));
    }

    @PostMapping("/logout/")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(Map.of("message", "Вышли успешно"));
    }
}
