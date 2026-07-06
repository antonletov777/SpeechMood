package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public User registerUser(String username, String rawPassword) {
        log.info("Попытка регистрации пользователя: {}", username);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с именем '" + username + "' уже существует");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));

        User savedUser = userRepository.save(user);
        log.info("Пользователь {} успешно зарегистрирован с ID: {}", username, savedUser.getId());
        return savedUser;
    }


    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("USER")
                .build();
    }

    @Transactional
    public User changeUsername(Long userId, String newUsername) {
        log.info("Запрос на смену имени для пользователя ID {}. Новое имя: {}", userId, newUsername);

        User user = getUserById(userId);

        if (!user.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Имя '" + newUsername + "' уже занято другим пользователем");
        }

        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Запрос на смену пароля для пользователя ID {}", userId);

        User user = getUserById(userId);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Старый пароль указан неверно!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Пароль для пользователя {} успешно изменен", user.getUsername());
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление аккаунта пользователя ID {}", userId);

        User user = getUserById(userId);
        userRepository.delete(user);

        log.info("Пользователь {} полностью удален", user.getUsername());
    }


    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(Math.toIntExact(id))
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь '" + username + "' не найден"));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл аватарки не может быть пустым");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Допустимые форматы изображения: JPEG, PNG, WEBP, GIF");
        }

        User user = getUserById(userId);

        try {
            Path dir = Path.of(uploadDir);
            Files.createDirectories(dir);

            String extension = switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };
            String fileName = UUID.randomUUID() + extension;
            Path target = dir.resolve(fileName);
            file.transferTo(target);

            String oldAvatarUrl = user.getAvatarUrl();
            user.setAvatarUrl("/uploads/avatars/" + fileName);
            User savedUser = userRepository.save(user);

            deleteOldAvatarFile(oldAvatarUrl);

            log.info("Аватар пользователя {} обновлен: {}", user.getUsername(), fileName);
            return savedUser;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить файл аватарки", e);
        }
    }

    private void deleteOldAvatarFile(String oldAvatarUrl) {
        if (oldAvatarUrl == null || !oldAvatarUrl.startsWith("/uploads/avatars/")) {
            return;
        }
        try {
            String oldFileName = oldAvatarUrl.substring("/uploads/avatars/".length());
            Files.deleteIfExists(Path.of(uploadDir).resolve(oldFileName));
        } catch (IOException e) {
            log.warn("Не удалось удалить старый файл аватарки {}: {}", oldAvatarUrl, e.getMessage());
        }
    }

}
