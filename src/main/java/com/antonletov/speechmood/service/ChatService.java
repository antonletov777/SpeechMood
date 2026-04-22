package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.Chat;
import com.antonletov.speechmood.model.ChatMessage;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.ChatMessageRepository;
import com.antonletov.speechmood.repository.ChatRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    //private final UserRepository userRepository;
    private final ChatMessageRepository messageRepository;


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



}