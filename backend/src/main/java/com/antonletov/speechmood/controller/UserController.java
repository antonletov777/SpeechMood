package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.ChangePasswordPayload;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.service.UserService;
import com.antonletov.speechmood.dto.UserDTO;
import org.springframework.http.ResponseEntity;
import com.antonletov.speechmood.service.FriendshipService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FriendshipService friendshipService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {

        User user = userService.getUserById(1L);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername()
        ));
    }


    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload, Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());

            userService.changePassword(
                    user.getId(),
                    payload.get("oldPassword"),
                    payload.get("newPassword")
            );

            return ResponseEntity.ok(Map.of("message", "Пароль успешно обновлен"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Внутренняя ошибка сервера"));
        }
    }


    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();

        // Преобразование из Entity в DTO
        List<UserDTO> userDTOs = users.stream()
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .build())
                .toList();

        return ResponseEntity.ok(userDTOs);
    }


    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/friends")
    public ResponseEntity<List<User>> getFriends(Principal principal) {

        User currentUser = userService.getUserByUsername(principal.getName());
        return ResponseEntity.ok(friendshipService.getFriends(currentUser.getId()));
    }
}
