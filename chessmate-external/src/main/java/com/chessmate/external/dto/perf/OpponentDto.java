package com.chessmate.external.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 상대방 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpponentDto(
    String id,
    String name,
    String title,         // GM, IM, FM, BOT 등
    String flair,
    Boolean patron,
    Integer patronColor
) {
}

