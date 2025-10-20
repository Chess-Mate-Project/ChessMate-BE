package backend.chessmate.global.common.response;

import lombok.Getter;


@Getter
public class ErrorResponse {
    private final boolean success = false;
    private final String code; // 에러 코드
    private final String message; // 사용자한테 보여줄 문구

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }
}

