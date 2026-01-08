package com.chessmate.infra_persistence.entity;


import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_color_stat")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class UserColorStatEntity {

  @Id
  @GeneratedValue
  private Long id;

  private Long userId;

  @Enumerated(EnumType.STRING)
  private ChessColor color; // BLACK / WHITE

  @Enumerated(EnumType.STRING)
  private GameResult result; // WIN / LOSE / DRAW
}
