package com.antonletov.speechmood.controller;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/")
    public ResponseEntity<List<Post>> getFeed() {
        return ResponseEntity.ok(postService.getGlobalFeed());
    }

    @PostMapping("/create/")
    public ResponseEntity<Post> create(@RequestBody Map<String, String> payload) {
        // Временно используем ID=1, пока не настроили получение текущего юзера из токена
        return ResponseEntity.ok(postService.createPost(1L, payload.get("content")));
    }

    @GetMapping("/{id}/")
    public ResponseEntity<Post> getById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @DeleteMapping("/{id}/delete/")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.deletePost(1L, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like/")
    public ResponseEntity<Map<String, Object>> like(@PathVariable Long id) {
        postService.toggleLike(1L, id);
        return ResponseEntity.ok(Map.of(
                "liked", postService.isLikedByUser(1L, id),
                "likes", postService.getLikesCount(id)
        ));
    }
}
