package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.ChangePasswordPayload;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 1. Получение данных о текущем пользователе
     * Соответствует вызову из base.html (checkAuthStatus)
     */
    @GetMapping("/me/")
    public ResponseEntity<?> getCurrentUser() {
        // Временно используем ID=1L, пока не внедрим JWT-фильтр.
        // Когда добавим Security, будем брать юзера из SecurityContextHolder.
        User user = userService.getUserById(1L);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername()
        ));
    }

    /**
     * 2. Смена пароля
     * Соответствует вызову из change-password.html
     */
    @PutMapping("/change-password/")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordPayload payload) {
        Long currentUserId = 1L; // Временно

        userService.changePassword(
                currentUserId,
                payload.getOldPassword(),
                payload.getNewPassword()
        );

        return ResponseEntity.ok(Map.of("message", "Пароль успешно изменен"));
    }

    /**
     * 3. Список всех пользователей
     * Полезно для страницы поиска или списка людей
     */
    @GetMapping("/")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * 4. Получение пользователя по ID
     */
    @GetMapping("/{id}/")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * 5. Удаление аккаунта
     */
    @DeleteMapping("/{id}/")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
