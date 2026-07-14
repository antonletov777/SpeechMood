package com.antonletov.speechmood.dto;

import lombok.Data;

@Data
public class MessagePayload {
    private String content;
    private Long post;
}