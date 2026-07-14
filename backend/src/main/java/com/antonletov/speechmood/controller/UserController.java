package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.ChangePasswordPayload;
import com.antonletov.speechmood.model.Friendship;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.service.UserService;
import com.antonletov.speechmood.dto.UserDTO;
import com.antonletov.speechmood.dto.UserProfileDTO;
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
    public ResponseEntity<UserProfileDTO> getCurrentUser(Principal principal) {
        User user = userService.getUserByUsername(principal.getName());
        return ResponseEntity.ok(UserProfileDTO.from(user));
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
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordPayload payload, Principal principal) {
        try {
            User user = userService.getUserByUsername(principal.getName());

            userService.changePassword(
                    user.getId(),
                    payload.getOldPassword(),
                    payload.getNewPassword()
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
        List<UserDTO> userDTOs = users.stream()
                .map(UserDTO::from)
                .toList();

        return ResponseEntity.ok(userDTOs);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(UserDTO.from(userService.getUserById(id)));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/friends")
    public ResponseEntity<List<UserDTO>> getFriends(Principal principal) {
        User currentUser = userService.getUserByUsername(principal.getName());
        List<UserDTO> friends = friendshipService.getFriends(currentUser.getId()).stream()
                .map(UserDTO::from)
                .toList();

        return ResponseEntity.ok(friends);
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
    public ResponseEntity<List<UserDTO>> getIncomingRequests(Principal principal) {
        User currentUser = userService.getUserByUsername(principal.getName());
        List<Friendship> requests = friendshipService.getIncomingRequests(currentUser.getId());
        List<UserDTO> result = requests.stream()
                .map(f -> UserDTO.from(f.getRequester()))
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
