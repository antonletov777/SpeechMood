package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.LoginPayload;
import com.antonletov.speechmood.dto.RegisterPayload;
import com.antonletov.speechmood.repository.BlacklistedTokenRepository;
import com.antonletov.speechmood.security.JwtService;
import com.antonletov.speechmood.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @PostMapping("/register/")
    public ResponseEntity<?> register(@RequestBody RegisterPayload payload) {
        userService.registerUser(payload.getUser().getUsername(), payload.getUser().getPassword());
        return ResponseEntity.ok(Map.of("message", "Успешно зарегистрировано!"));
    }

    @PostMapping("/login/")
    public ResponseEntity<?> login(@RequestBody LoginPayload payload) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(payload.getUsername(), payload.getPassword())
        );

        String accessToken = jwtService.generateToken(payload.getUsername());
        String refreshToken = jwtService.generateRefreshToken(payload.getUsername());

        return ResponseEntity.ok(Map.of(
                "access", accessToken,
                "refresh", refreshToken
        ));
    }

    @PostMapping("/token/refresh/")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refresh");
        String username = jwtService.extractUsername(refreshToken);

        if (jwtService.isTokenValid(refreshToken, username)) {
            String newAccessToken = jwtService.generateToken(username);
            return ResponseEntity.ok(Map.of("access", newAccessToken));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
    }

    @PostMapping("/logout/")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);

            if (!blacklistedTokenRepository.existsByToken(jwt)) {
                blacklistedTokenRepository.save(new com.antonletov.speechmood.model.BlacklistedToken(jwt));
            }
        }

        return ResponseEntity.ok(Map.of("message", "Вышли успешно"));
    }
}