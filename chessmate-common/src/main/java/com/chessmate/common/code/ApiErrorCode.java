package com.chessmate.common.code;

public enum ApiErrorCode implements ErrorCode {
  API_RATE_LIMIT(429, "Api 호출 제한 (1분 기다리기)");

  private final int statusCode;
  private final String message;

  ApiErrorCode(int statusCode, String message) {
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
