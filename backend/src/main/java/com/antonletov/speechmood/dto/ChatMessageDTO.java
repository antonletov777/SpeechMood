package com.antonletov.speechmood.dto;

import com.antonletov.speechmood.model.ChatMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageDTO {
    private String content;
    private String type;
    private String audioUrl;
    private LocalDateTime timestamp;
    private UserDTO author;

    public static ChatMessageDTO from(ChatMessage message) {
        return ChatMessageDTO.builder()
                .content(message.getContent())
                .type(message.getType().name())
                .audioUrl(message.getAudioUrl())
                .timestamp(message.getSentAt())
                .author(message.getSender() != null ? UserDTO.from(message.getSender()) : null)
                .build();
    }
}
