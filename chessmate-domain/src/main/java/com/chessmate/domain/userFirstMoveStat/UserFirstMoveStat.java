package com.chessmate.domain.userFirstMoveStat;

public class UserFirstMoveStat {
  private Long id;
  private Long userId;
  private String firstMove;


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

  public static class Builder {
    private Long id;
    private Long userId;
    private String firstMove;

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

    public UserFirstMoveStat build() {
      return new UserFirstMoveStat(this);
    }
  }
}
