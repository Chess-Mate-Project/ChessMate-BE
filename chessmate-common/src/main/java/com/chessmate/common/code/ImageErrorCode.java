package com.chessmate.common.code;

import java.io.File;

public enum ImageErrorCode implements ErrorCode {

  FILENAME_IS_NOT_MISSING(400, "이미지 파일이 첨부되지 않았습니다."),
  CONTENT_TYPE_IS_NOT_IMAGE(400, "이미지 파일 형식이 올바르지 않습니다."),
  UNSUPPORTED_FILE_TYPE(400, "지원하지 않는 파일 형식입니다."),
      FILE_UPLOAD_TO_CLOUDFLARE_R2_FAILED(500, "파일을 Cloudflare R2에 업로드하는 데 실패했습니다.");

  private final int statusCode;
  private final String message;

  ImageErrorCode(int statusCode, String message) {
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
