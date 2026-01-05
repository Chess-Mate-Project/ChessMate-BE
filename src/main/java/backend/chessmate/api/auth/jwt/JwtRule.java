package backend.chessmate.api.auth.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * JWT 처리에 사용될
 * - 쿠키 이름(prefix)
 * - 응답 헤더 이름
 * 을 한 곳에 정의한 enum
 */
@RequiredArgsConstructor
@Getter
public enum JwtRule {
    JWT_ISSUE_HEADER("Set-Cookie"),   // 토큰 발급 시 HTTP 헤더 이름
    ACCESS_PREFIX("access"),          // Access Token 쿠키 이름
    REFRESH_PREFIX("refresh");        // Refresh Token 쿠키 이름

    private final String value;
}