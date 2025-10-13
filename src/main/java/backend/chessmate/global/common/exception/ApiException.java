package backend.chessmate.global.common.exception;

import backend.chessmate.global.common.code.ErrorCode;
import lombok.Getter;

@Getter

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

    public ApiException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
