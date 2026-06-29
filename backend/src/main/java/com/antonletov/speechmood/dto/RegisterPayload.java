package com.antonletov.speechmood.dto;

import lombok.Data;

@Data
public class RegisterPayload {
    private String username;
    private String email;
    private String password;
}
