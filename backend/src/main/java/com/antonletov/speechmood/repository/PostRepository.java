package com.antonletov.speechmood.repository;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findAllByAuthorOrderByCreatedAtDesc(User author);
}