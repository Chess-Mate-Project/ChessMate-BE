//package com.chessmate.infra_persistence.entity;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Index;
//import jakarta.persistence.Table;
//import java.time.LocalDate;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
///**
// * Lichess 사용자의 일일 스트릭 엔티티
// * 각 사용자의 날짜별 게임 전적을 기록합니다.
// */
//@Entity
//@Table(name = "lichess_user_daily_streaks", indexes = {
//    @Index(name = "idx_user_date", columnList = "user_id, date"),
//    @Index(name = "idx_date", columnList = "date")
//})
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//@Getter
//@Setter
//public class LichessUserDailyStreakEntity {
//
//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  private Long id;
//
//  @Column(name = "user_id", nullable = false)
//  private Long userId;
//
//  @Column(name = "date", nullable = false)
//  private LocalDate date;
//
//  @Column(name = "win", nullable = false)
//  private int win;
//
//  @Column(name = "lose", nullable = false)
//  private int lose;
//
//  @Column(name = "draw", nullable = false)
//  private int draw;
//
//  @Column(name = "last_game_at")
//  private Long lastGameAt;
//
//  @Column(name = "last_rating")
//  private int lastRating;
//}
//
