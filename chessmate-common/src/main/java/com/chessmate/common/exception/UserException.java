package com.chessmate.common.exception;

import com.chessmate.common.code.ErrorCode;

public class UserException extends RuntimeException {

  private final ErrorCode errorCode;

  public UserException(final ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
