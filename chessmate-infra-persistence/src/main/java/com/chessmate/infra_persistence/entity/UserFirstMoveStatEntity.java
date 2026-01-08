package com.chessmate.infra_persistence.entity;

import jakarta.persistence.*;
import lombok.*;

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
}
