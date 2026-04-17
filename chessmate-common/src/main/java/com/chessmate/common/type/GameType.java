package com.chessmate.common.type;

public enum GameType {
  RAPID,
  BLITZ,
  CLASSICAL,
  BULLET;

  public String toTimeClass() {
    return this.name().toLowerCase();
  }

  public static GameType fromTimeClass(String timeClass) {
    return valueOf(timeClass.toUpperCase());
  }
}
