package com.antonletov.speechmood.dto;

import lombok.Data;

@Data
public class LoginPayload {
    private String username;
    private String password;
}
