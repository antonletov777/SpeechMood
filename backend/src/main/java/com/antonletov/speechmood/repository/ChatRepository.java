package com.antonletov.speechmood.repository;

import com.antonletov.speechmood.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {
}
