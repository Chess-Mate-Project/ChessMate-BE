//package com.chessmate.infra_persistence.entity;
//
//import com.chessmate.common.type.ChessColor;
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
// * Lichess 사용자의 첫 수 별 게임 통계 엔티티
// */
//@Entity
//@Table(name = "lichess_user_first_move_stats", indexes = {
//    @Index(name = "idx_user_color_move", columnList = "user_id, color, first_move")
//})
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//@Getter
//@Setter
//public class LichessUserFirstMoveStatEntity {
//
//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  private Long id;
//
//  @Column(name = "user_id", nullable = false)
//  private Long userId;
//
//  @Column(name = "first_move", nullable = false)
//  private String firstMove;
//
//  @Column(name = "color", nullable = false)
//  @Enumerated(EnumType.STRING)
//  private ChessColor color;
//
//  @Column(name = "game_type", nullable = false)
//  @Enumerated(EnumType.STRING)
//  private GameType gameType;
//}

