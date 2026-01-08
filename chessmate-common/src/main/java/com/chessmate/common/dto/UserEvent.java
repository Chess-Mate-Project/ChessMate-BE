package com.chessmate.common.dto;

import java.time.LocalDateTime;

public record UserEvent (
    String type,
    Long userId,
    String lichessToken
) {}
