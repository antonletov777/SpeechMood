package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.model.PostLike;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.PostLikeRepository;
import com.antonletov.speechmood.repository.PostRepository;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    @Transactional
    public Post createPost(Long authorId, String content) {
        log.info("Пользователь {} создает новый пост", authorId);

        User author = userRepository.findById(Math.toIntExact(authorId))
                .orElseThrow(() -> new EntityNotFoundException("Автор не найден"));

        Post post = new Post();
        post.setAuthor(author);
        post.setContent(content);

        return postRepository.save(post);
    }


    @Transactional(readOnly = true)
    public List<Post> getGlobalFeed() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }


    @Transactional
    public void toggleLike(Long userId, Long postId) {
        User user = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Пост не найден"));

        Optional<PostLike> existingLike = postLikeRepository.findByUserAndPost(user, post);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            log.info("Пользователь {} убрал лайк с поста {}", userId, postId);
        } else {
            PostLike newLike = new PostLike();
            newLike.setUser(user);
            newLike.setPost(post);
            postLikeRepository.save(newLike);
            log.info("Пользователь {} лайкнул пост {}", userId, postId);
        }
    }

    @Transactional(readOnly = true)
    public long getLikesCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Пост не найден"));
        return postLikeRepository.countByPost(post);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow();
    }

    public boolean isLikedByUser(Long userId, Long postId) {
        User user = userRepository.findById(userId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        return postLikeRepository.findByUserAndPost(user, post).isPresent();
    }

    public void deletePost(Long userId, Long postId) {
        Post post = getPostById(postId);
        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Нет прав на удаление");
        }
        postRepository.delete(post);
    }
}