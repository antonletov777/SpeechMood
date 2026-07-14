package com.antonletov.speechmood.dto;

import com.antonletov.speechmood.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileDTO {
    private Long id;
    private String username;
    private String firstName;
    private String gender;
    private Integer age;
    private String avatarUrl;

    public static UserProfileDTO from(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .gender(user.getGender())
                .age(user.getAge())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
