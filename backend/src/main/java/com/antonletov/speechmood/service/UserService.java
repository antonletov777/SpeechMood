package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

}
