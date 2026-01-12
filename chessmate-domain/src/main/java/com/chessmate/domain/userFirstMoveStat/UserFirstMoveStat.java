package com.chessmate.domain.userFirstMoveStat;

import com.chessmate.common.type.GameType;

public class UserFirstMoveStat {
  private Long id;
  private Long userId;
  private String firstMove;
  private GameType gameType; // BULLET / BLITZ / RAPID / CLASSICAL

  public UserFirstMoveStat() {}

  private UserFirstMoveStat(Builder builder) {
    this.id = builder.id;
    this.userId = builder.userId;
    this.firstMove = builder.firstMove;
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

    public Builder gameType(GameType gameType) {
      this.gameType = gameType;
      return this;
    }

    public UserFirstMoveStat build() {
      return new UserFirstMoveStat(this);
    }
  }
}
