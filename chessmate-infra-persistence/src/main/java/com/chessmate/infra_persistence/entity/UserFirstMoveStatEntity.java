package com.chessmate.infra_persistence.entity;

import com.chessmate.common.type.GameType;
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
@Table(name = "user_first_move_stat")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class UserFirstMoveStatEntity {

  @Id @GeneratedValue
  private Long id;

  private Long userId;

  private String firstMove; // e4, d4 등

  @Enumerated(EnumType.STRING)
  private GameType gameType; // BULLET / BLITZ / RAPID / CLASSICAL
}
