package com.chessmate.common.response;

public class SuccessResponse<T> {

  private final boolean success;
  private final String message;
  private final T data;

  public SuccessResponse(String message, T data) {
    this.success = true;
    this.message = message;
    this.data = data;
  }

  public boolean isSuccess() {
    return success;
  }

  public boolean getSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}
