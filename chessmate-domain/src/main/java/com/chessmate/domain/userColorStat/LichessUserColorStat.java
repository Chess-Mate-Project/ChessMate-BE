package com.chessmate.domain.userColorStat;

import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameResult;
import com.chessmate.common.type.GameType;

public class LichessUserColorStat {
  private final Long id;
  private final Long userId;
  private final ChessColor color;
  private final GameResult result;
  private GameType gameType; // BULLET / BLITZ / RAPID / CLASSICAL

  private LichessUserColorStat(Builder builder) {
    this.id = builder.id;
    this.userId = builder.userId;
    this.color = builder.color;
    this.result = builder.result;
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

  public ChessColor getColor() {
    return color;
  }

  public GameResult getResult() {
    return result;
  }

  public GameType getGameType() {
    return gameType;
  }

  public static class Builder {
    private Long id;
    private Long userId;
    private ChessColor color;
    private GameResult result;
    private GameType gameType;

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder userId(Long userId) {
      this.userId = userId;
      return this;
    }

    public Builder color(ChessColor color) {
      this.color = color;
      return this;
    }

    public Builder result(GameResult result) {
      this.result = result;
      return this;
    }

    public Builder gameType(GameType gameType) {
      this.gameType = gameType;
      return this;
    }

    public LichessUserColorStat build() {
      return new LichessUserColorStat(this);
    }
  }
}

