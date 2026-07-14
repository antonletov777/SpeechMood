package com.antonletov.speechmood.dto;

import lombok.Data;

@Data
public class ChangePasswordPayload {
    private String oldPassword;
    private String newPassword;
}