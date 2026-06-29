package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.dto.MessagePayload;
import com.antonletov.speechmood.model.PostComment;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.service.PostCommentService;
import com.antonletov.speechmood.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final PostCommentService commentService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<PostComment> create(@RequestBody MessagePayload payload, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());

        return ResponseEntity.ok(commentService.addComment(
                user.getId(),
                payload.getPost(),
                payload.getContent()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostComment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getCommentById(id));
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());

        commentService.deleteComment(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}