package com.antonletov.speechmood.dto;

import com.antonletov.speechmood.model.Chat;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatDetailDTO {
    private Long id;
    private String name;
    private UserDTO author;
    private List<UserDTO> members;
    private List<ChatMessageDTO> messages;

    public static ChatDetailDTO from(Chat chat) {
        return ChatDetailDTO.builder()
                .id(chat.getId())
                .name(chat.getTitle())
                .author(chat.getCreator() != null ? UserDTO.from(chat.getCreator()) : null)
                .members(chat.getParticipants().stream().map(UserDTO::from).toList())
                .messages(chat.getMessages().stream().map(ChatMessageDTO::from).toList())
                .build();
    }
}
