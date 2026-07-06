package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.ChangePasswordPayload;
import com.antonletov.speechmood.model.Friendship;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.service.UserService;
import com.antonletov.speechmood.dto.UserDTO;
import org.springframework.http.ResponseEntity;
import com.antonletov.speechmood.service.FriendshipService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        User user = userService.getUserByUsername(principal.getName());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("firstName", user.getFirstName());
        result.put("gender", user.getGender());
        result.put("age", user.getAge());
        result.put("avatarUrl", user.getAvatarUrl());
        return ResponseEntity.ok(result);
    }


    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());
            User updatedUser = userService.updateAvatar(user.getId(), file);

            return ResponseEntity.ok(Map.of("avatarUrl", updatedUser.getAvatarUrl()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка при загрузке аватара: ", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Не удалось загрузить аватар"));
        }
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
                        .avatarUrl(user.getAvatarUrl())
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

    @PostMapping("/{id}/follow")
    public ResponseEntity<?> followUser(@PathVariable Long id, Principal principal) {
        try {

            User currentUser = userService.getUserByUsername(principal.getName());

            friendshipService.sendRequest(currentUser.getId(), id);

            return ResponseEntity.ok(Map.of("message", "Вы успешно подписались на пользователя"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка при подписке: ", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Ошибка при выполнении подписки"));
        }
    }

    @GetMapping("/friend-requests")
    public ResponseEntity<List<Map<String, Object>>> getIncomingRequests(Principal principal) {
        User currentUser = userService.getUserByUsername(principal.getName());
        List<Friendship> requests = friendshipService.getIncomingRequests(currentUser.getId());
        List<Map<String, Object>> result = requests.stream()
                .map(f -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", f.getRequester().getId());
                    map.put("username", f.getRequester().getUsername());
                    map.put("avatarUrl", f.getRequester().getAvatarUrl());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/accept-request")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id, Principal principal) {
        User currentUser = userService.getUserByUsername(principal.getName());
        friendshipService.acceptRequest(currentUser.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Заявка принята"));
    }

    @PostMapping("/{id}/decline-request")
    public ResponseEntity<?> declineRequest(@PathVariable Long id, Principal principal) {
        User currentUser = userService.getUserByUsername(principal.getName());
        friendshipService.declineRequest(currentUser.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Заявка отклонена"));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getFriendshipStatus(@PathVariable Long id, Principal principal) {
        User currentUser = userService.getUserByUsername(principal.getName());
        User targetUser = userService.getUserById(id);

        String status = friendshipService.getFriendshipStatus(currentUser, targetUser);
        return ResponseEntity.ok(Map.of("status", status));
    }
}
