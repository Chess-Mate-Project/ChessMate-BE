package com.chessmate.common.code;

public enum UserErrorCode implements ErrorCode {

  NOT_FOUND_USER(404, "사용자를 찾을 수 없습니다."),
  NOT_SUPPORT_GAME_TYPE(400, "지원하지 않는 게임 타입입니다."),
  NOT_FOUND_USER_OAUTH(404, "OAuth 사용자 정보를 찾을 수 없습니다."),
  FAILD_GET_USER_PERF(500, "사용자의 퍼포먼스를 가져오는 데 실패했습니다."),
  FAILD_GET_USER_GAMES(500, "사용자의 게임 정보를 가져오는 데 실패했습니다."),
  FAILD_GET_USER_ACCOUNT(500, "사용자의 계정 정보를 가져오는 데 실패했습니다."),
  FAILD_GET_USER_RATING_HISTORY(500, "사용자의 레이팅 히스토리를 가져오는 데 실패했습니다.");

  private final int statusCode;
  private final String message;

  UserErrorCode(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  @Override
  public int getStatusCode() {
    return this.statusCode;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
