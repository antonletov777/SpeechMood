package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.model.PostComment;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.PostCommentRepository;
import com.antonletov.speechmood.repository.PostRepository;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostCommentService postCommentService;

    private User postAuthor;
    private User commentAuthor;
    private Post post;

    @BeforeEach
    void setUp() {
        postAuthor = new User();
        postAuthor.setId(1L);

        commentAuthor = new User();
        commentAuthor.setId(2L);

        post = new Post();
        post.setId(100L);
        post.setAuthor(postAuthor);
    }

    @Test
    void addComment_shouldSaveComment_whenAuthorAndPostExist() {
        when(userRepository.findById(2)).thenReturn(Optional.of(commentAuthor));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(postCommentRepository.save(any(PostComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostComment result = postCommentService.addComment(2L, 100L, "nice post");

        assertThat(result.getAuthor()).isEqualTo(commentAuthor);
        assertThat(result.getPost()).isEqualTo(post);
        assertThat(result.getContent()).isEqualTo("nice post");
    }

    @Test
    void addComment_shouldThrow_whenAuthorNotFound() {
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommentService.addComment(2L, 100L, "nice post"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(postCommentRepository, never()).save(any());
    }

    @Test
    void addComment_shouldThrow_whenPostNotFound() {
        when(userRepository.findById(2)).thenReturn(Optional.of(commentAuthor));
        when(postRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommentService.addComment(2L, 100L, "nice post"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(postCommentRepository, never()).save(any());
    }

    @Test
    void getCommentsForPost_shouldReturnCommentsOrderedByCreatedAtAsc() {
        PostComment comment = new PostComment();
        comment.setPost(post);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(postCommentRepository.findAllByPostOrderByCreatedAtAsc(post)).thenReturn(List.of(comment));

        List<PostComment> result = postCommentService.getCommentsForPost(100L);

        assertThat(result).containsExactly(comment);
    }

    @Test
    void getCommentsForPost_shouldThrow_whenPostNotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommentService.getCommentsForPost(100L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteComment_shouldDelete_whenRequesterIsCommentAuthor() {
        PostComment comment = new PostComment();
        comment.setId(5L);
        comment.setAuthor(commentAuthor);
        comment.setPost(post);

        when(postCommentRepository.findById(5L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(2)).thenReturn(Optional.of(commentAuthor));

        postCommentService.deleteComment(2L, 5L);

        verify(postCommentRepository).delete(comment);
    }

    @Test
    void deleteComment_shouldDelete_whenRequesterIsPostAuthor() {
        PostComment comment = new PostComment();
        comment.setId(5L);
        comment.setAuthor(commentAuthor);
        comment.setPost(post);

        when(postCommentRepository.findById(5L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1)).thenReturn(Optional.of(postAuthor));

        postCommentService.deleteComment(1L, 5L);

        verify(postCommentRepository).delete(comment);
    }

    @Test
    void deleteComment_shouldThrow_whenRequesterIsNeitherAuthor() {
        PostComment comment = new PostComment();
        comment.setId(5L);
        comment.setAuthor(commentAuthor);
        comment.setPost(post);

        User stranger = new User();
        stranger.setId(3L);

        when(postCommentRepository.findById(5L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(3)).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> postCommentService.deleteComment(3L, 5L))
                .isInstanceOf(AccessDeniedException.class);

        verify(postCommentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_shouldThrow_whenCommentNotFound() {
        when(postCommentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommentService.deleteComment(2L, 5L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getCommentById_shouldReturnComment_whenExists() {
        PostComment comment = new PostComment();
        comment.setId(5L);

        when(postCommentRepository.findById(5L)).thenReturn(Optional.of(comment));

        PostComment result = postCommentService.getCommentById(5L);

        assertThat(result).isEqualTo(comment);
    }

    @Test
    void getCommentById_shouldThrow_whenNotFound() {
        when(postCommentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommentService.getCommentById(5L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
