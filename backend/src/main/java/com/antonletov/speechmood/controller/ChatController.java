package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.ChatDetailDTO;
import com.antonletov.speechmood.dto.ChatSummaryDTO;
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
    public ResponseEntity<List<ChatSummaryDTO>> getUserGroups(Principal principal) {
        User currentUser = userService.getUserByUsername(principal.getName());
        List<ChatSummaryDTO> result = chatService.getUserGroups(currentUser.getId()).stream()
                .map(ChatSummaryDTO::from)
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroup(@PathVariable Long id, Principal principal) {
        try {
            Chat group = chatService.getGroupById(id);
            return ResponseEntity.ok(ChatDetailDTO.from(group));
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
