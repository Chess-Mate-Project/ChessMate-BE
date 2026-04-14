package com.chessmate.worker;

import com.chessmate.external.dto.chesscom.ChesscomTokenResponse;
import com.chessmate.external.service.OAuthService;
import com.chessmate.infra_redis.token.PlatformTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Chess.com Access Token 자동 갱신 컴포넌트.
 *
 * Worker에서 401 응답 수신 시 이 클래스를 통해 Refresh Token으로 재발급 후 Redis에 저장합니다.
 * Refresh Token이 없거나 만료된 경우 null을 반환 → Worker가 TOKEN_EXPIRED 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChesscomTokenRefresher {

    private final PlatformTokenStore tokenStore;
    private final OAuthService oAuthService;

    /**
     * userId의 Refresh Token으로 새 Access Token을 발급받아 Redis에 저장합니다.
     *
     * @return 새 Access Token, 갱신 실패 시 null
     */
    public String refresh(Long userId) {
        String refreshToken = tokenStore.getChesscomRefreshToken(userId);
        if (refreshToken == null) {
            log.warn("[ChesscomTokenRefresher] Refresh Token 없음 userId={}", userId);
            return null;
        }

        try {
            ChesscomTokenResponse response = oAuthService.refreshChesscomToken(refreshToken);
            tokenStore.saveChesscomTokens(
                userId,
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getExpiresIn()
            );
            log.info("[ChesscomTokenRefresher] 토큰 갱신 성공 userId={}", userId);
            return response.getAccessToken();
        } catch (Exception e) {
            log.error("[ChesscomTokenRefresher] 토큰 갱신 실패 userId={} error={}", userId, e.getMessage());
            return null;
        }
    }
}