package backend.chessmate.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApiErrorCode implements ErrorCode {
    API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "Api 호출 제한 (1분 기다리기)");
    private final HttpStatus httpStatus;
    private final String message;

}
