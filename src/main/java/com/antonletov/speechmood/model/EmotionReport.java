package com.antonletov.speechmood.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "emotion_reports")
@Getter
@Setter
@NoArgsConstructor
public class EmotionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String emotion;
    private Double accuracy;
    private String audioFileUrl;

    private LocalDateTime analyzedAt = LocalDateTime.now();
}