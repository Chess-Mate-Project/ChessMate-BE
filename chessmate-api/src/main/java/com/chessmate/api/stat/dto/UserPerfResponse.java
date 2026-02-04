package com.chessmate.api.stat.dto;

import com.chessmate.common.type.GameType;

/**
 * 게임 타입별 상세 퍼포먼스 정보 응답
 */
public record UserPerfResponse(
    // 기본 레이팅 정보
    int rating,
    int gamesPlayed,
    boolean prov,

    // 게임 통계
    int all,              // 전체 게임 수
    int rated,            // 레이티드 게임 수
    int wins,             // 승리 횟수
    int losses,           // 패배 횟수
    int draws,            // 무승부 횟수
    int tour,             // 토너먼트 게임 수
    int berserk,          // 광폭 모드 사용 수
    double opAvg,         // 상대방 평균 레이팅
    int seconds,          // 총 게임 시간 (초)
    int disconnects,      // 연결 끊김 수

    // 레이팅 관련
    int highestRating,    // 최고 레이팅
    int lowestRating,     // 최저 레이팅
    int maxStreak,        // 최대 연승
    int maxLossStreak,    // 최대 연패
    boolean uncertain      // 티어 불확실성 (rated < 50이면 true)
) {
}

