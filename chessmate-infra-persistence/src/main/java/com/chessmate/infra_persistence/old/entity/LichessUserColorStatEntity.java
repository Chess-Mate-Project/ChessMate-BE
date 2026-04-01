//package com.chessmate.infra_persistence.entity;
//
//import com.chessmate.common.type.ChessColor;
//import com.chessmate.common.type.GameResult;
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
// * Lichess 사용자의 색상별(백/흑) 게임 결과 통계 엔티티
// */
//@Entity
//@Table(name = "lichess_user_color_stats", indexes = {
//    @Index(name = "idx_user_color_result", columnList = "user_id, color, result")
//})
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//@Getter
//@Setter
//public class LichessUserColorStatEntity {
//
//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  private Long id;
//
//  @Column(name = "user_id", nullable = false)
//  private Long userId;
//
//  @Column(name = "color", nullable = false)
//  @Enumerated(EnumType.STRING)
//  private ChessColor color;
//
//  @Column(name = "result", nullable = false)
//  @Enumerated(EnumType.STRING)
//  private GameResult result;
//
//  @Column(name = "game_type", nullable = false)
//  @Enumerated(EnumType.STRING)
//  private GameType gameType;
//}
//
