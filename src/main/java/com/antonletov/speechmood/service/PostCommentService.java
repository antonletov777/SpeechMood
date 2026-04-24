package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.model.PostComment;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.PostCommentRepository;
import com.antonletov.speechmood.repository.PostRepository;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostComment addComment(Long authorId, Long postId, String content) {
        log.info("Пользователь {} оставляет комментарий к посту {}", authorId, postId);

        User author = userRepository.findById(Math.toIntExact(authorId))
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Пост не найден"));

        PostComment comment = new PostComment();
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setContent(content);

        return postCommentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<PostComment> getCommentsForPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Пост не найден"));

        return postCommentRepository.findAllByPostOrderByCreatedAtAsc(post);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Запрос на удаление комментария {} от пользователя {}", commentId, userId);

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден"));

        User requestingUser = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));


        boolean isCommentAuthor = comment.getAuthor().getId().equals(userId);
        boolean isPostAuthor = comment.getPost().getAuthor().getId().equals(userId);

        if (!isCommentAuthor && !isPostAuthor) {
            log.warn("Пользователь {} попытался удалить чужой комментарий {}", userId, commentId);
            throw new AccessDeniedException("У вас нет прав для удаления этого комментария");
        }

        postCommentRepository.delete(comment);
        log.info("Комментарий {} успешно удален", commentId);
    }

    @Transactional(readOnly = true)
    public PostComment getCommentById(Long id) {
        return postCommentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден"));
    }
}
