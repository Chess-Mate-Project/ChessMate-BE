//package com.chessmate.infra_persistence.entity;
//
//import com.chessmate.common.type.GameType;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EnumType;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Index;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
///**
// * Lichess 사용자의 종목별 성능 데이터 엔티티
// * (bullet, blitz, rapid, classical 등)
// */
//@Entity
//@Table(name = "lichess_user_perfs", indexes = {
//    @Index(name = "idx_user_game_type", columnList = "user_id, game_type")
//})
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//@Getter
//@Setter
//public class LichessUserPerfEntity {
//
//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  private Long id;
//
//  @Column(name = "user_id", nullable = false)
//  private Long userId;
//
//  @Column(name = "game_type", nullable = false)
//  @Enumerated(EnumType.STRING)
//  private GameType gameType;
//
//  @Column(name = "rating", nullable = false)
//  private int rating;
//
//  @Column(name = "rd")
//  private double rd;
//
//  @Column(name = "prog")
//  private int prog;
//
//  @Column(name = "games_played", nullable = false)
//  private int gamesPlayed;
//
//  @Column(name = "prov")
//  private boolean prov;
//
//  @Column(name = "all")
//  private int all;
//
//  @Column(name = "rated")
//  private int rated;
//
//  @Column(name = "wins", nullable = false)
//  private int wins;
//
//  @Column(name = "losses", nullable = false)
//  private int losses;
//
//  @Column(name = "draws", nullable = false)
//  private int draws;
//
//  @Column(name = "tour")
//  private int tour;
//
//  @Column(name = "berserk")
//  private int berserk;
//
//  @Column(name = "op_avg")
//  private double opAvg;
//
//  @Column(name = "seconds")
//  private int seconds;
//
//  @Column(name = "disconnects")
//  private int disconnects;
//
//  @Column(name = "highest_rating")
//  private int highestRating;
//
//  @Column(name = "lowest_rating")
//  private int lowestRating;
//
//  @Column(name = "max_streak")
//  private int maxStreak;
//
//  @Column(name = "max_loss_streak")
//  private int maxLossStreak;
//
//  @Column(name = "uncertain")
//  private boolean uncertain;
//}
//
