package com.chessmate.infra_persistence.entity;

import com.chessmate.common.type.GameType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_perfs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserPerfEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "game_type", nullable = false)
  private GameType gameType;

  @Column(name = "rating", nullable = false)
  private int rating;

  @Column(name = "games_played", nullable = false)
  private int gamesPlayed;

  @Column(name = "prov", nullable = false)
  private boolean prov;

  @Column(name = "all_games", nullable = false)
  private int all;

  @Column(name = "rated_games", nullable = false)
  private int rated;

  @Column(name = "wins", nullable = false)
  private int wins;

  @Column(name = "losses", nullable = false)
  private int losses;

  @Column(name = "draws", nullable = false)
  private int draws;

  @Column(name = "tour", nullable = false)
  private int tour;

  @Column(name = "berserk", nullable = false)
  private int berserk;

  @Column(name = "op_avg", nullable = false)
  private double opAvg;

  @Column(name = "seconds", nullable = false)
  private int seconds;

  @Column(name = "disconnects", nullable = false)
  private int disconnects;

  @Column(name = "highest_rating", nullable = false)
  private int highestRating;

  @Column(name = "lowest_rating", nullable = false)
  private int lowestRating;

  @Column(name = "max_streak", nullable = false)
  private int maxStreak;

  @Column(name = "max_loss_streak", nullable = false)
  private int maxLossStreak;

  @Column(name = "uncertain", nullable = false)
  private boolean uncertain;
}
