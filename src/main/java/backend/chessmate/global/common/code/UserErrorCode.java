package backend.chessmate.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    NOT_SUPPORT_GAME_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 게임 타입입니다."),
    NOT_FOUND_USER_OAUTH(HttpStatus.NOT_FOUND, "OAuth 사용자 정보를 찾을 수 없습니다."),
    FAILD_GET_USER_PERF(HttpStatus.INTERNAL_SERVER_ERROR, "사용자의 퍼포먼스를 가져오는 데 실패했습니다."),
    FAILD_GET_USER_GAMES(HttpStatus.INTERNAL_SERVER_ERROR, "사용자의 게임 정보를 가져오는 데 실패했습니다."),
    FAILD_GET_USER_ACCOUNT(HttpStatus.INTERNAL_SERVER_ERROR, "사용자의 계정 정보를 가져오는 데 실패했습니다."),
    FAILD_GET_USER_RATING_HISTORY(HttpStatus.INTERNAL_SERVER_ERROR, "사용자의 레이팅 히스토리를 가져오는 데 실패했습니다.");
    private final HttpStatus httpStatus;
    private final String message;
}
