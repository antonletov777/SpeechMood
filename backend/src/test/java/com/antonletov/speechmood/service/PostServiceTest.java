package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.model.PostLike;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.PostLikeRepository;
import com.antonletov.speechmood.repository.PostRepository;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setUsername("author");

        post = new Post();
        post.setId(10L);
        post.setAuthor(author);
        post.setContent("hello world");
    }

    @Test
    void createPost_shouldSaveAndReturnPost_whenAuthorExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.createPost(1L, "hello world");

        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getContent()).isEqualTo("hello world");
    }

    @Test
    void createPost_shouldThrow_whenAuthorNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(1L, "hello world"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void getGlobalFeed_shouldReturnPostsOrderedByCreatedAtDesc() {
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(post));

        List<Post> result = postService.getGlobalFeed();

        assertThat(result).containsExactly(post);
    }

    @Test
    void toggleLike_shouldCreateLike_whenNotAlreadyLiked() {
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserAndPost(author, post)).thenReturn(Optional.empty());

        postService.toggleLike(1L, 10L);

        verify(postLikeRepository).save(any(PostLike.class));
        verify(postLikeRepository, never()).delete(any());
    }

    @Test
    void toggleLike_shouldRemoveLike_whenAlreadyLiked() {
        PostLike existingLike = new PostLike();
        existingLike.setUser(author);
        existingLike.setPost(post);

        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserAndPost(author, post)).thenReturn(Optional.of(existingLike));

        postService.toggleLike(1L, 10L);

        verify(postLikeRepository).delete(existingLike);
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    void toggleLike_shouldThrow_whenPostNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.toggleLike(1L, 10L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getLikesCount_shouldReturnCountFromRepository() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.countByPost(post)).thenReturn(5L);

        long result = postService.getLikesCount(10L);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void getLikesCount_shouldThrow_whenPostNotFound() {
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getLikesCount(10L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getPostById_shouldReturnPost_whenExists() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        Post result = postService.getPostById(10L);

        assertThat(result).isEqualTo(post);
    }

    @Test
    void getPostById_shouldThrow_whenNotFound() {
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(10L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void isLikedByUser_shouldReturnTrue_whenLikeExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserAndPost(author, post)).thenReturn(Optional.of(new PostLike()));

        boolean result = postService.isLikedByUser(1L, 10L);

        assertThat(result).isTrue();
    }

    @Test
    void isLikedByUser_shouldReturnFalse_whenLikeDoesNotExist() {
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserAndPost(author, post)).thenReturn(Optional.empty());

        boolean result = postService.isLikedByUser(1L, 10L);

        assertThat(result).isFalse();
    }

    @Test
    void deletePost_shouldDeletePost_whenRequestedByAuthor() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        postService.deletePost(1L, 10L);

        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_shouldThrow_whenRequestedByNonAuthor() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(2L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нет прав");

        verify(postRepository, never()).delete(any());
    }
}
