package com.chessmate.common.response;

public class ErrorResponse {

  private final boolean success = false;
  private final int code; // 에러 코드
  private final String message; // 사용자한테 보여줄 문구

  public ErrorResponse(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public boolean isSuccess() {
    return success;
  }

  // 일부 호출자가 getSuccess()를 기대할 수 있으므로 호환성 위해 추가
  public boolean getSuccess() {
    return success;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}



