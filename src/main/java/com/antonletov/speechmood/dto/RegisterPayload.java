package com.antonletov.speechmood.dto;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class RegisterPayload {
    private Integer age;
    private String gender;
    private UserDto user;

    @Data
    static public class UserDto {
        private String username;
        private String email;
        private String password;
    }
}
