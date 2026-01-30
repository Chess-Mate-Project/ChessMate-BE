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

  // 도메인 객체로 변환
  public com.chessmate.domain.userPerf.UserPerf toDomain() {
    return com.chessmate.domain.userPerf.UserPerf.builder()
        .userId(this.userId)
        .gameType(this.gameType)
        .rating(this.rating)
        .gamesPlayed(this.gamesPlayed)
        .prov(this.prov)
        .all(this.all)
        .rated(this.rated)
        .wins(this.wins)
        .losses(this.losses)
        .draws(this.draws)
        .tour(this.tour)
        .berserk(this.berserk)
        .opAvg(this.opAvg)
        .seconds(this.seconds)
        .disconnects(this.disconnects)
        .highestRating(this.highestRating)
        .lowestRating(this.lowestRating)
        .maxStreak(this.maxStreak)
        .maxLossStreak(this.maxLossStreak)
        .uncertain(this.uncertain)
        .build();
  }

  // 도메인 객체로부터 엔티티 생성
  public static UserPerfEntity fromDomain(com.chessmate.domain.userPerf.UserPerf domain) {
    return UserPerfEntity.builder()
        .userId(domain.getUserId())
        .gameType(domain.getGameType())
        .rating(domain.getRating())
        .gamesPlayed(domain.getGamesPlayed())
        .prov(domain.isProv())
        .all(domain.getAll())
        .rated(domain.getRated())
        .wins(domain.getWins())
        .losses(domain.getLosses())
        .draws(domain.getDraws())
        .tour(domain.getTour())
        .berserk(domain.getBerserk())
        .opAvg(domain.getOpAvg())
        .seconds(domain.getSeconds())
        .disconnects(domain.getDisconnects())
        .highestRating(domain.getHighestRating())
        .lowestRating(domain.getLowestRating())
        .maxStreak(domain.getMaxStreak())
        .maxLossStreak(domain.getMaxLossStreak())
        .uncertain(domain.isUncertain())
        .build();
  }
}
