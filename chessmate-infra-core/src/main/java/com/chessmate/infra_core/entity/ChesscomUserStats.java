package com.chessmate.infra_core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chesscom_user_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChesscomUserStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Integer playerId;

    @Column(name = "game_type", nullable = false)
    private String gameType;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false)
    private Integer rd;

    @Column(name = "rated_games_count")
    private Integer ratedGamesCount;

    @Column(nullable = false)
    private Integer win;

    @Column(nullable = false)
    private Integer loss;

    @Column(nullable = false)
    private Integer draw;

    @Column(name = "best_rating")
    private Integer bestRating;

    @Column(name = "best_rating_date")
    private LocalDateTime bestRatingDate;

    @Column(name = "best_game_url")
    private String bestGameUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

