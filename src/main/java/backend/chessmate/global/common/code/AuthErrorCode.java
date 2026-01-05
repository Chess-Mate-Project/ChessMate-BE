package backend.chessmate.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    FAILD_GET_OAUTH_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "OAuth Access Token을 가져오는 데 실패했습니다."),
    FAILD_GET_USER_ACCOUNT(HttpStatus.UNAUTHORIZED, "사용자 계정 정보를 가져오는 데 실패했습니다."),
    JWT_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "JWT 토큰이 존재하지 않습니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
    private final HttpStatus httpStatus;
    private final String message;
}
