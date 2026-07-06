package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.Chat;
import com.antonletov.speechmood.model.ChatMessage;
import com.antonletov.speechmood.model.MessageType;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.ChatMessageRepository;
import com.antonletov.speechmood.repository.ChatRepository;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/webm", "audio/ogg", "audio/mpeg", "audio/mp4", "audio/wav", "audio/x-m4a"
    );

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository messageRepository;

    @Value("${app.upload.voice-dir}")
    private String voiceDir;


    @Transactional
    public Chat createChat(String title, Long creatorId, Set<Long> participantIds) {
        log.info("Пользователь {} создает чат '{}'", creatorId, title);

        User creator = getUser(creatorId);

        Set<User> participants = new HashSet<>();
        participants.add(creator);

        for (Long id : participantIds) {
            participants.add(getUser(id));
        }

        Chat chat = new Chat();
        chat.setTitle(title);
        chat.setParticipants(participants);

        return chatRepository.save(chat);
    }


    @Transactional
    public Chat createGroupChat(String title, Long creatorId, Set<Long> participantIds) {
        log.info("Пользователь {} создает группу '{}'", creatorId, title);

        User creator = getUser(creatorId);

        Set<User> participants = new HashSet<>();
        participants.add(creator);
        for (Long id : participantIds) {
            participants.add(getUser(id));
        }

        Chat chat = new Chat();
        chat.setTitle(title);
        chat.setGroupChat(true);
        chat.setCreator(creator);
        chat.setParticipants(participants);

        return chatRepository.save(chat);
    }

    @Transactional(readOnly = true)
    public List<Chat> getUserGroups(Long userId) {
        User user = getUser(userId);
        return chatRepository.findAllByParticipantsContainingAndIsGroupChatTrue(user);
    }


    @Transactional
    public void addParticipant(Long chatId, Long inviterId, Long newUserId) {
        log.info("Пользователь {} приглашает {} в чат {}", inviterId, newUserId, chatId);

        Chat chat = getChat(chatId);
        User inviter = getUser(inviterId);
        User newUser = getUser(newUserId);

        if (!chat.getParticipants().contains(inviter)) {
            throw new IllegalStateException("Вы не можете приглашать людей в чат, в котором не состоите");
        }

        if (chat.getParticipants().contains(newUser)) {
            throw new IllegalStateException("Пользователь уже состоит в этом чате");
        }

        chat.getParticipants().add(newUser);
        chatRepository.save(chat);
    }

    @Transactional
    public void removeParticipant(Long chatId, Long requesterId, Long userIdToRemove) {
        log.info("Запрос на удаление пользователя {} из чата {}", userIdToRemove, chatId);

        Chat chat = getChat(chatId);
        User requester = getUser(requesterId);
        User userToRemove = getUser(userIdToRemove);

        if (!requesterId.equals(userIdToRemove)) {
            throw new IllegalStateException("Вы можете удалить только самого себя (выйти из чата)");
        }

        if (!chat.getParticipants().contains(userToRemove)) {
            throw new IllegalStateException("Пользователь не состоит в этом чате");
        }

        chat.getParticipants().remove(userToRemove);

        if (chat.getParticipants().isEmpty()) {
            chatRepository.delete(chat);
            log.info("Чат {} удален, так как в нем не осталось участников", chatId);
        } else {
            chatRepository.save(chat);
        }
    }

    @Transactional
    public ChatMessage sendTextMessage(Long chatId, Long senderId, String content) {
        log.info("Пользователь {} пишет в чат {}", senderId, chatId);

        Chat chat = getChat(chatId);
        User sender = getUser(senderId);

        if (!chat.getParticipants().contains(sender)) {
            throw new IllegalStateException("Вы не являетесь участником этого чата");
        }

        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(content);

        return messageRepository.save(message);
    }

    @Transactional
    public ChatMessage sendVoiceMessage(Long chatId, Long senderId, MultipartFile audioFile) {
        log.info("Пользователь {} отправляет голосовое сообщение в чат {}", senderId, chatId);

        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Аудиофайл не может быть пустым");
        }

        String contentType = audioFile.getContentType();
        if (contentType == null || !ALLOWED_AUDIO_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Неподдерживаемый формат аудио");
        }

        Chat chat = getChat(chatId);
        User sender = getUser(senderId);

        if (!chat.getParticipants().contains(sender)) {
            throw new IllegalStateException("Вы не являетесь участником этого чата");
        }

        try {
            Path dir = Path.of(voiceDir);
            Files.createDirectories(dir);

            String extension = switch (contentType) {
                case "audio/ogg" -> ".ogg";
                case "audio/mpeg" -> ".mp3";
                case "audio/mp4", "audio/x-m4a" -> ".m4a";
                case "audio/wav" -> ".wav";
                default -> ".webm";
            };
            String fileName = UUID.randomUUID() + extension;
            audioFile.transferTo(dir.resolve(fileName));

            ChatMessage message = new ChatMessage();
            message.setChat(chat);
            message.setSender(sender);
            message.setType(MessageType.VOICE);
            message.setAudioUrl("/uploads/voice/" + fileName);

            return messageRepository.save(message);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить голосовое сообщение", e);
        }
    }

    @Transactional(readOnly = true)
    public Chat getGroupById(Long chatId) {
        Chat chat = getChat(chatId);
        if (!chat.isGroupChat()) {
            throw new EntityNotFoundException("Группа с ID " + chatId + " не найдена");
        }
        return chat;
    }

    @Transactional
    public void addGroupMembers(Long chatId, Long requesterId, Set<Long> memberIds) {
        Chat chat = getChat(chatId);
        User requester = getUser(requesterId);

        if (!chat.getParticipants().contains(requester)) {
            throw new IllegalStateException("Вы не являетесь участником этого чата");
        }

        for (Long memberId : memberIds) {
            User newUser = getUser(memberId);
            if (!chat.getParticipants().contains(newUser)) {
                chat.getParticipants().add(newUser);
            }
        }
        chatRepository.save(chat);
    }

    @Transactional
    public void removeGroupMember(Long chatId, Long requesterId, Long memberIdToRemove) {
        Chat chat = getChat(chatId);
        User requester = getUser(requesterId);
        User memberToRemove = getUser(memberIdToRemove);

        boolean isCreator = chat.getCreator() != null && chat.getCreator().getId().equals(requester.getId());
        if (!isCreator) {
            throw new IllegalStateException("Только создатель группы может удалять участников");
        }

        if (memberToRemove.getId().equals(chat.getCreator().getId())) {
            throw new IllegalStateException("Нельзя удалить создателя группы");
        }

        chat.getParticipants().remove(memberToRemove);
        chatRepository.save(chat);
    }

    private User getUser(Long userId) {
        return userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден"));
    }

    private Chat getChat(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Чат с ID " + chatId + " не найден"));
    }

}