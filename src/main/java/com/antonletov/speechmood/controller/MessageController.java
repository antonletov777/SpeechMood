package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.MessagePayload;
import com.antonletov.speechmood.model.PostComment;
import com.antonletov.speechmood.service.PostCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final PostCommentService commentService;

    @PostMapping("/create/")
    public ResponseEntity<PostComment> create(@RequestBody MessagePayload payload) {
        return ResponseEntity.ok(commentService.addComment(
                payload.getAuthor().getId(),
                payload.getPost(),
                payload.getContent()
        ));
    }

    @GetMapping("/{id}/")
    public ResponseEntity<PostComment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getCommentById(id));
    }

    @DeleteMapping("/{id}/delete/")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // to do настроить получение из токена id пользователя, соответственно нужно подключить Spring Security
        Long currentUserId = 1L;

        commentService.deleteComment(currentUserId, id);
        return ResponseEntity.noContent().build();
    }
}