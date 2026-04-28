package com.antonletov.speechmood.repository;

import com.antonletov.speechmood.model.Post;
import com.antonletov.speechmood.model.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    List<PostComment> findAllByPostOrderByCreatedAtAsc(Post post);

    long countByPost(Post post);
}