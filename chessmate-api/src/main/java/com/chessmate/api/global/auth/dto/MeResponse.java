package com.chessmate.api.global.auth.dto;

import com.chessmate.common.dto.OAuthPlatForm;

/**
 * GET /api/auth/me 응답 DTO.
 *
 * 프론트엔드가 현재 인증 상태와 플랫폼을 판단하기 위한 정보를 담습니다.
 * 이 엔드포인트가 200을 반환하면 인증된 상태, 401이면 미인증 상태입니다.
 */
public record MeResponse(
    Long userId,
    String username,
    OAuthPlatForm platform,
    String profileImageUrl,
    String description
) {}