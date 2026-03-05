package com.chessmate.infra_core.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lichess_streak")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichessStreakEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lichess_id", nullable = false)
    private String lichessId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer win;

    @Column(nullable = false)
    private Integer loss;

    @Column(nullable = false)
    private Integer draw;

    @Column(nullable = false)
    private Integer total;

    @Column(name = "last_rating")
    private Integer lastRating;

    @Column(name = "last_game_time")
    private LocalDateTime lastGameTime;
}

