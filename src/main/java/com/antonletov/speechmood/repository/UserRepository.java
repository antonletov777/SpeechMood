package com.antonletov.speechmood.repository;

import com.antonletov.speechmood.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
