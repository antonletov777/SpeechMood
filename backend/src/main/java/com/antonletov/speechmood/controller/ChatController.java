package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.model.Chat;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.service.ChatService;
import com.antonletov.speechmood.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            String name = (String) payload.get("name");
            List<?> rawIds = (List<?>) payload.getOrDefault("memberIds", List.of());

            Set<Long> memberIds = new HashSet<>();
            for (Object id : rawIds) {
                memberIds.add(((Number) id).longValue());
            }

            User currentUser = userService.getUserByUsername(principal.getName());
            Chat chat = chatService.createGroupChat(name, currentUser.getId(), memberIds);

            return ResponseEntity.ok(Map.of("id", chat.getId(), "message", "Группа успешно создана"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка при создании группы: ", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Ошибка создания группы"));
        }
    }
}
