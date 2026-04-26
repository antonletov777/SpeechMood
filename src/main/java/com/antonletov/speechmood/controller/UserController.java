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

    @GetMapping("/me/")
    public ResponseEntity<?> getCurrentUser() {

        User user = userService.getUserById(1L);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername()
        ));
    }


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


    @GetMapping("/")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @GetMapping("/{id}/")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }


    @DeleteMapping("/{id}/")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
