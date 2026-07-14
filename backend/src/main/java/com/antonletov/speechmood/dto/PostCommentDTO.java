package com.antonletov.speechmood.dto;

import com.antonletov.speechmood.model.PostComment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PostCommentDTO {
    private Long id;
    private Long postId;
    private UserDTO author;
    private String content;
    private LocalDateTime createdAt;

    public static PostCommentDTO from(PostComment comment) {
        return PostCommentDTO.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .author(UserDTO.from(comment.getAuthor()))
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
