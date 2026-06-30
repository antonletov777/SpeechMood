package com.antonletov.speechmood.repository;

import com.antonletov.speechmood.model.Chat;
import com.antonletov.speechmood.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findAllByParticipantsContainingAndIsGroupChatTrue(User user);
}
