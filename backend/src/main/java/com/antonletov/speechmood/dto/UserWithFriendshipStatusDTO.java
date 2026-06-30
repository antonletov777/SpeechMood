package com.antonletov.speechmood.dto;

import com.antonletov.speechmood.model.FriendshipStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserWithFriendshipStatusDTO {
    private Long id;
    private String username;
    private FriendshipStatus friendStatus;
}