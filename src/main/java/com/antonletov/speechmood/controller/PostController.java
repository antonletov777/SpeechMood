package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.service.PostService;
import com.antonletov.speechmood.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Post>> getFeed() {
        return ResponseEntity.ok(postService.getGlobalFeed());
    }

    @PostMapping("/create")
    public ResponseEntity<Post> create(@RequestBody Map<String, String> payload, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());

        return ResponseEntity.ok(postService.createPost(user.getId(), payload.get("content")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> getById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());

        postService.deletePost(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> like(@PathVariable Long id, Principal principal) {
        User user = userService.getUserByUsername(principal.getName());

        postService.toggleLike(user.getId(), id);
        return ResponseEntity.ok(Map.of(
                "liked", postService.isLikedByUser(user.getId(), id),
                "likes", postService.getLikesCount(id)
        ));
    }
}