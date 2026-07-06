package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.model.Chat;
import com.antonletov.speechmood.model.ChatMessage;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.service.ChatService;
import com.antonletov.speechmood.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
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

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUserGroups(Principal principal) {
        User currentUser = userService.getUserByUsername(principal.getName());
        List<Chat> groups = chatService.getUserGroups(currentUser.getId());
        List<Map<String, Object>> result = groups.stream()
                .map(g -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", g.getId());
                    map.put("name", g.getTitle());
                    if (g.getCreator() != null) {
                        map.put("author", Map.of("username", g.getCreator().getUsername()));
                    }
                    return map;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroup(@PathVariable Long id, Principal principal) {
        try {
            Chat group = chatService.getGroupById(id);

            Map<String, Object> result = new HashMap<>();
            result.put("id", group.getId());
            result.put("name", group.getTitle());

            if (group.getCreator() != null) {
                result.put("author", Map.of("username", group.getCreator().getUsername()));
            }

            List<Map<String, Object>> members = group.getParticipants().stream()
                    .map(u -> {
                        Map<String, Object> member = new HashMap<>();
                        member.put("id", u.getId());
                        member.put("username", u.getUsername());
                        member.put("avatarUrl", u.getAvatarUrl());
                        return member;
                    })
                    .toList();
            result.put("members", members);

            List<Map<String, Object>> messages = group.getMessages().stream()
                    .map(m -> {
                        Map<String, Object> msg = new HashMap<>();
                        msg.put("content", m.getContent());
                        msg.put("type", m.getType().name());
                        msg.put("audioUrl", m.getAudioUrl());
                        msg.put("timestamp", m.getSentAt());
                        if (m.getSender() != null) {
                            msg.put("author", Map.of("username", m.getSender().getUsername()));
                        }
                        return msg;
                    })
                    .toList();
            result.put("messages", messages);

            return ResponseEntity.ok(result);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/messages/send")
    public ResponseEntity<?> sendMessage(@PathVariable Long id,
                                         @RequestBody Map<String, String> payload,
                                         Principal principal) {
        try {
            User currentUser = userService.getUserByUsername(principal.getName());
            String content = payload.get("content");
            ChatMessage message = chatService.sendTextMessage(id, currentUser.getId(), content);
            return ResponseEntity.ok(Map.of("id", message.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/messages/send-voice")
    public ResponseEntity<?> sendVoiceMessage(@PathVariable Long id,
                                              @RequestParam("file") MultipartFile file,
                                              Principal principal) {
        try {
            User currentUser = userService.getUserByUsername(principal.getName());
            ChatMessage message = chatService.sendVoiceMessage(id, currentUser.getId(), file);
            return ResponseEntity.ok(Map.of("id", message.getId(), "audioUrl", message.getAudioUrl()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/members/add")
    public ResponseEntity<?> addMembers(@PathVariable Long id,
                                        @RequestBody Map<String, Object> payload,
                                        Principal principal) {
        try {
            User currentUser = userService.getUserByUsername(principal.getName());
            List<?> rawIds = (List<?>) payload.getOrDefault("memberIds", List.of());
            Set<Long> memberIds = new HashSet<>();
            for (Object rawId : rawIds) {
                memberIds.add(((Number) rawId).longValue());
            }
            chatService.addGroupMembers(id, currentUser.getId(), memberIds);
            return ResponseEntity.ok(Map.of("message", "Участники добавлены"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/members/remove")
    public ResponseEntity<?> removeMember(@PathVariable Long id,
                                          @RequestBody Map<String, Object> payload,
                                          Principal principal) {
        try {
            User currentUser = userService.getUserByUsername(principal.getName());
            List<?> rawIds = (List<?>) payload.getOrDefault("memberIds", List.of());
            for (Object rawId : rawIds) {
                chatService.removeGroupMember(id, currentUser.getId(), ((Number) rawId).longValue());
            }
            return ResponseEntity.ok(Map.of("message", "Участник удалён"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable Long id, Principal principal) {
        try {
            User currentUser = userService.getUserByUsername(principal.getName());
            chatService.removeParticipant(id, currentUser.getId(), currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "Вы вышли из группы"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

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
