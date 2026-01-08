package com.chessmate.common.exception;


import com.chessmate.common.code.ErrorCode;

public class AuthException extends RuntimeException {

  private final ErrorCode errorCode;


  public AuthException(final ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
