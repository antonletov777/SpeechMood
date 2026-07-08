package com.antonletov.speechmood.model;

import com.antonletov.speechmood.enums.FriendshipStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships")
@Getter
@Setter
@NoArgsConstructor
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}