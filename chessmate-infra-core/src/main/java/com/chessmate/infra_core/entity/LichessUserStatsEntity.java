package com.chessmate.infra_core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lichess_user_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichessUserStatsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lichess_id", nullable = false)
    private String lichessId;

    @Column(name = "game_type", nullable = false)
    private String gameType;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false)
    private Float rd;

    @Column(nullable = false)
    private Integer prog;

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

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

