package com.chessmate.api.user.dto;

import java.time.LocalDateTime;

public record ProfileResponse (
    String username,
    String description,
    LocalDateTime createdAt,
    LocalDateTime lichessCreatedAt,
    String lichessLink
) {

}
