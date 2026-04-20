package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Chess.com 공개 프로필 응답 DTO
 * GET /pub/player/{username}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChesscomPublicProfileResponse(
    @JsonProperty("player_id") Long playerId,
    String username,
    /** 계정 가입 시각 (epoch seconds) */
    Long joined
) {}
