package com.chessmate.api.user.dto;

import com.chessmate.common.dto.OAuthPlatForm;
import java.time.LocalDateTime;

public record ProfileResponse(
    Long id,
    String username,
    OAuthPlatForm platform,
    String description,
    String profileImageUrl,
    String bannerImageUrl,
    LocalDateTime createdAt,
    /** 플랫폼(Lichess/Chess.com) 계정 가입일 — 스트릭 콤보박스 기준점 */
    LocalDateTime platformJoinedAt
) {}
