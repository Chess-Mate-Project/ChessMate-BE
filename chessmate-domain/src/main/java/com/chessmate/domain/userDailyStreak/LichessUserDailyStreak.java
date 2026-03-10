package com.chessmate.domain.userDailyStreak;

import java.time.LocalDate;

public class LichessUserDailyStreak {
  private Long id;
  private Long userId;
  private LocalDate date;
  private int win;
  private int lose;
  private int draw;
  private Long lastGameAt;
  private int lastRating; // 마지막 게임 후 정산된 레이팅

  // 무인자 생성자(프레임워크/직렬화용)
  public LichessUserDailyStreak() {}

  private LichessUserDailyStreak(Builder builder) {
    this.id = builder.id;
    this.userId = builder.userId;
    this.date = builder.date;
    this.win = builder.win;
    this.lose = builder.lose;
    this.draw = builder.draw;
    this.lastGameAt = builder.lastGameAt;
    this.lastRating = builder.lastRating;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public LocalDate getDate() {
    return date;
  }

  public int getWin() {
    return win;
  }

  public int getLose() {
    return lose;
  }

  public int getDraw() {
    return draw;
  }

  public Long getLastGameAt() {
    return lastGameAt;
  }

  public int getLastRating() {
    return lastRating;
  }

  // 추가된 Setter들
  public void setId(Long id) {
    this.id = id;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public void setWin(int win) {
    this.win = win;
  }

  public void setLose(int lose) {
    this.lose = lose;
  }

  public void setDraw(int draw) {
    this.draw = draw;
  }

  public void setLastGameAt(Long lastGameAt) {
    this.lastGameAt = lastGameAt;
  }

  public void setLastRating(int lastRating) {
    this.lastRating = lastRating;
  }

  public static class Builder {
    private Long id;
    private Long userId;
    private LocalDate date;
    private int win;
    private int lose;
    private int draw;
    private Long lastGameAt;
    private int lastRating;

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder userId(Long userId) {
      this.userId = userId;
      return this;
    }

    public Builder date(LocalDate date) {
      this.date = date;
      return this;
    }

    public Builder win(int win) {
      this.win = win;
      return this;
    }

    public Builder lose(int lose) {
      this.lose = lose;
      return this;
    }

    public Builder draw(int draw) {
      this.draw = draw;
      return this;
    }

    public Builder lastGameAt(Long lastGameAt) {
      this.lastGameAt = lastGameAt;
      return this;
    }

    public Builder lastRating(int lastRating) {
      this.lastRating = lastRating;
      return this;
    }

    public LichessUserDailyStreak build() {
      return new LichessUserDailyStreak(this);
    }
  }
}

