package com.antonletov.speechmood.dto;

import com.antonletov.speechmood.model.Chat;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSummaryDTO {
    private Long id;
    private String name;
    private UserDTO author;

    public static ChatSummaryDTO from(Chat chat) {
        return ChatSummaryDTO.builder()
                .id(chat.getId())
                .name(chat.getTitle())
                .author(chat.getCreator() != null ? UserDTO.from(chat.getCreator()) : null)
                .build();
    }
}
