package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @TempDir
    Path tempDir;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("anton");
        user.setPassword("encoded-old-password");

        ReflectionTestUtils.setField(userService, "uploadDir", tempDir.toString());
    }

    @Test
    void registerUser_shouldSaveUser_whenUsernameIsFree() {
        when(userRepository.existsByUsername("anton")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerUser("anton", "raw-password");

        assertThat(result.getUsername()).isEqualTo("anton");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_shouldThrow_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("anton")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser("anton", "raw-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anton");

        verify(userRepository, never()).save(any());
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        when(userRepository.findByUsername("anton")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("anton");

        assertThat(result.getUsername()).isEqualTo("anton");
        assertThat(result.getPassword()).isEqualTo("encoded-old-password");
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void changeUsername_shouldUpdateUsername_whenNewNameIsFree() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("new-name")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.changeUsername(1L, "new-name");

        assertThat(result.getUsername()).isEqualTo("new-name");
    }

    @Test
    void changeUsername_shouldThrow_whenNewNameIsTaken() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken-name")).thenReturn(true);

        assertThatThrownBy(() -> userService.changeUsername(1L, "taken-name"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUsername_shouldNotCheckUniqueness_whenNameIsUnchanged() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.changeUsername(1L, "anton");

        assertThat(result.getUsername()).isEqualTo("anton");
        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    void changePassword_shouldUpdatePassword_whenOldPasswordMatches() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");

        userService.changePassword(1L, "old-password", "new-password");

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_shouldThrow_whenOldPasswordIsWrong() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-old-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrong-password", "new-password"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void getUserById_shouldReturnUser_whenExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void getUserById_shouldThrow_whenNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getUserByUsername_shouldReturnUser_whenExists() {
        when(userRepository.findByUsername("anton")).thenReturn(Optional.of(user));

        User result = userService.getUserByUsername("anton");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void getUserByUsername_shouldThrow_whenNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUsername("ghost"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.getAllUsers();

        assertThat(result).containsExactly(user);
    }

    @Test
    void updateAvatar_shouldThrow_whenFileIsNull() {
        assertThatThrownBy(() -> userService.updateAvatar(1L, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).findById(any(Integer.class));
    }

    @Test
    void updateAvatar_shouldThrow_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> userService.updateAvatar(1L, emptyFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAvatar_shouldThrow_whenContentTypeIsUnsupported() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.txt", "text/plain", "not-an-image".getBytes());

        assertThatThrownBy(() -> userService.updateAvatar(1L, file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAvatar_shouldSaveFileAndUpdateUrl_whenFileIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image-bytes".getBytes());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateAvatar(1L, file);

        assertThat(result.getAvatarUrl()).startsWith("/uploads/avatars/").endsWith(".png");
        String savedFileName = result.getAvatarUrl().substring("/uploads/avatars/".length());
        assertThat(Files.exists(tempDir.resolve(savedFileName))).isTrue();
    }

    @Test
    void updateAvatar_shouldDeleteOldAvatarFile_whenPreviousAvatarExisted() throws Exception {
        Path oldAvatar = tempDir.resolve("old-avatar.jpg");
        Files.write(oldAvatar, "old-bytes".getBytes());
        user.setAvatarUrl("/uploads/avatars/old-avatar.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image-bytes".getBytes());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateAvatar(1L, file);

        assertThat(Files.exists(oldAvatar)).isFalse();
    }
}
