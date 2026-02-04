package com.chessmate.api.user.dto;

import java.time.LocalDateTime;

public record ProfileResponse (
    // 기본 정보
    Long id,
    String username,
    String lichessId,
    String title,
    String description,

    // 프로필 이미지
    String profileImage,
    String bannerImage,

    // 날짜
    LocalDateTime createdAt,
    LocalDateTime lichessCreatedAt,

    // 게임 통계 (전체)
    int allGames,
    int ratedGames,
    int wins,
    int losses,
    int draws,
    int totalSeconds
) {

}
