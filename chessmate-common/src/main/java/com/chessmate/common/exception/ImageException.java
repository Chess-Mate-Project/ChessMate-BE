package com.chessmate.common.exception;

import com.chessmate.common.code.ErrorCode;

public class ImageException extends RuntimeException {

  private final ErrorCode errorCode;

  public ImageException(final ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
