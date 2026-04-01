// File: `chessmate-common/src/main/java/com/chessmate/common/code/AuthErrorCode.java`
package com.chessmate.common.code;

public enum AuthErrorCode implements ErrorCode {

  FAILD_GET_OAUTH_ACCESS_TOKEN(401, "OAuth Access Token을 가져오는 데 실패했습니다."),
  FAILD_GET_USER_ACCOUNT(401, "사용자 계정 정보를 가져오는 데 실패했습니다."),
  JWT_TOKEN_NOT_FOUND(401, "JWT 토큰이 존재하지 않습니다."),
  INVALID_REFRESH_TOKEN(401, "유효하지 않은 리프레시 토큰입니다."),
  INVALID_PROVIDER(400, "유효하지 않은 OAuth2 공급자입니다."),
  OAUTH_CODE_VERIFIER_NOT_FOUND(401, "OAuth 검증 코드가 만료되었거나 존재하지 않습니다. 다시 로그인해주세요."),
  OAUTH_CALLBACK_FAILED(400, "OAuth 콜백 처리에 실패했습니다."),
  OAUTH_REFRESH_FAILED(400, "OAuth 토큰 갱신에 실패했습니다.");

  private final int statusCode;
  private final String message;

  AuthErrorCode(int statusCode, String message) {
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
