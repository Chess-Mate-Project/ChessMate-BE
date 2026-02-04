package com.chessmate.common.code;

public enum AuthSuccessCode implements SuccessCode {

  LOGIN_SUCCESS(200, "로그인 성공");

  private final int statusCode;
  private final String message;

  AuthSuccessCode(int statusCode, String message) {
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
