package com.chessmate.infra_persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "user_daily_streak")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class UserDailyStreakEntity {

  @Id @GeneratedValue
  private Long id;

  private Long userId;

  private LocalDate date;

  private int win;
  private int lose;
  private int draw;

  private Long lastGameAt; // 증분 처리용
}
