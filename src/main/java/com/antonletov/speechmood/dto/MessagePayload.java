package com.antonletov.speechmood.dto;

import lombok.Data;

@Data
public class MessagePayload {
    private String content;
    private String timestamp;
    private Long post;
    private AuthorDto author;

    @Data
    public static class AuthorDto {
        private Long id;
        private String username;
        private String email;
    }
}