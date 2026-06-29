package com.antonletov.speechmood.repository;

import com.antonletov.speechmood.model.Friendship;
import com.antonletov.speechmood.model.FriendshipStatus;
import com.antonletov.speechmood.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequesterAndAddressee(User requester, User addressee);

    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester = :u1 AND f.addressee = :u2) OR " +
            "(f.requester = :u2 AND f.addressee = :u1)")
    Optional<Friendship> findAnyRelationship(@Param("u1") User u1, @Param("u2") User u2);

    List<Friendship> findAllByAddresseeAndStatus(User addressee, FriendshipStatus status);
}