package com.antonletov.speechmood.dto;

import com.antonletov.speechmood.model.Post;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PostDTO {
    private Long id;
    private UserDTO author;
    private String content;
    private LocalDateTime createdAt;
    private int likesCount;
    private int commentsCount;

    public static PostDTO from(Post post) {
        return PostDTO.builder()
                .id(post.getId())
                .author(UserDTO.from(post.getAuthor()))
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .likesCount(post.getLikes().size())
                .commentsCount(post.getComments().size())
                .build();
    }
}
