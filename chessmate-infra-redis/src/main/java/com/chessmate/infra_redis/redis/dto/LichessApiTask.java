package com.chessmate.infra_redis.redis.dto;

import java.io.Serializable;
import lombok.Builder;

@Builder
public record LichessApiTask(
    Long userId,
    TaskType type,
    String lichessToken,
    String username,
    boolean isFullSync // true면 전체기록(Heavy), false면 단순업데이트(Light)
) implements Serializable {}
