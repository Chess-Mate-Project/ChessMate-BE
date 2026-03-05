package com.chessmate.infra_core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lichess_id")
    private String lichessId;

    @Column(length = 255)
    private String username;

    @Column(length = 500)
    private String description;

    @Column(name = "banner_image")
    private String bannerImage;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(length = 10)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "lichess_created_at")
    private LocalDateTime lichessCreatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 게임 통계
    @Column(name = "all_games")
    private int allGames;

    @Column(name = "rated_games")
    private int ratedGames;

    @Column(name = "wins")
    private int wins;

    @Column(name = "losses")
    private int losses;

    @Column(name = "draws")
    private int draws;

    @Column(name = "total_seconds")
    private int totalSeconds;
}

