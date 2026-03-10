package com.chessmate.domain.userFirstMoveStat;

import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameType;

public class LichessUserFirstMoveStat {
  private Long id;
  private Long userId;
  private String firstMove;
  private ChessColor color; // WHITE / BLACK
  private GameType gameType; // BULLET / BLITZ / RAPID / CLASSICAL

  public LichessUserFirstMoveStat() {}

  private LichessUserFirstMoveStat(Builder builder) {
    this.id = builder.id;
    this.userId = builder.userId;
    this.firstMove = builder.firstMove;
    this.color = builder.color;
    this.gameType = builder.gameType;
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

  public String getFirstMove() {
    return firstMove;
  }

  public ChessColor getColor() {
    return color;
  }

  // 추가된 Setter들
  public void setId(Long id) {
    this.id = id;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public void setFirstMove(String firstMove) {
    this.firstMove = firstMove;
  }

  public void setColor(ChessColor color) {
    this.color = color;
  }

  public GameType getGameType() {
    return gameType;
  }

  public void setGameType(GameType gameType) {
    this.gameType = gameType;
  }

  public static class Builder {
    private Long id;
    private Long userId;
    private String firstMove;
    private ChessColor color;
    private GameType gameType; // BULLET / BLITZ / RAPID / CLASSICAL

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder userId(Long userId) {
      this.userId = userId;
      return this;
    }

    public Builder firstMove(String firstMove) {
      this.firstMove = firstMove;
      return this;
    }

    public Builder color(ChessColor color) {
      this.color = color;
      return this;
    }

    public Builder gameType(GameType gameType) {
      this.gameType = gameType;
      return this;
    }

    public LichessUserFirstMoveStat build() {
      return new LichessUserFirstMoveStat(this);
    }
  }
}

