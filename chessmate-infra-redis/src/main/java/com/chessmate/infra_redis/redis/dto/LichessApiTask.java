package com.chessmate.infra_redis.redis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record LichessApiTask(
    Long userId,
    TaskType type,
    String lichessToken,
    String username,
    boolean isFullSync, // true면 전체기록(Heavy), false면 단순업데이트(Light)

        String batchId, //UUID
    String taskId //UUID
 ) implements Serializable {}
