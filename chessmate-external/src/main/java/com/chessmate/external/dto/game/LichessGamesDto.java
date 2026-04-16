package com.chessmate.external.dto.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lichess 게임 목록 DTO
 *
 * - Lichess API에서 반환되는 게임 목록 데이터를 담는 DTO
 * - 전역 SNAKE_CASE Jackson 설정을 무시하고 camelCase 필드명 그대로 매핑
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LichessGamesDto(

    String id,

    boolean rated,

    String variant,

    String perf,

    @JsonProperty("createdAt")
    Long createdAt,

    @JsonProperty("lastMoveAt")
    Long lastMoveAt,

    String status,

    String winner,

    String moves,

    Players players
) {}
