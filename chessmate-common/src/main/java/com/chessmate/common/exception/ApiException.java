package com.chessmate.common.exception;


import com.chessmate.common.code.ErrorCode;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public ApiException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
