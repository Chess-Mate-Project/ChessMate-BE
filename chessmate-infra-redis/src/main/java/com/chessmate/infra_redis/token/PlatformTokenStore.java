 package com.chessmate.infra_redis.token;

import com.chessmate.infra_redis.repository.AuthRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Worker에서 사용하는 플랫폼 토큰 저장소 파사드.
 * AuthRedisRepository를 위임하여 Worker가 Redis 키 구조를 직접 알 필요가 없도록 합니다.
 */
@Component
@RequiredArgsConstructor
public class PlatformTokenStore {

    private final AuthRedisRepository authRedisRepository;

    // ======================== Lichess ========================

    public String getLichessAccessToken(Long userId) {
        return authRedisRepository.getLichessAccessToken(userId);
    }

    public void saveLichessAccessToken(Long userId, String token, int expiresInSeconds) {
        authRedisRepository.saveLichessAccessToken(userId, token, expiresInSeconds);
    }

    // ======================== Chess.com ========================

    public String getChesscomAccessToken(Long userId) {
        return authRedisRepository.getChesscomAccessToken(userId);
    }

    public String getChesscomRefreshToken(Long userId) {
        return authRedisRepository.getChesscomRefreshToken(userId);
    }

    public void saveChesscomTokens(Long userId, String accessToken, String refreshToken, int expiresInSeconds) {
        authRedisRepository.saveChesscomAccessToken(userId, accessToken, expiresInSeconds);
        // Refresh Token TTL: 30일 고정
        authRedisRepository.saveChesscomRefreshToken(userId, refreshToken, 30 * 24 * 3600);
    }
}