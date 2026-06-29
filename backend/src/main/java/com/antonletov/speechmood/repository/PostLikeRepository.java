package com.antonletov.speechmood.repository;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.model.PostLike;
import com.antonletov.speechmood.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserAndPost(User user, Post post);

    long countByPost(Post post);
}