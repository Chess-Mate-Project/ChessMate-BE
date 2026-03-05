package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 게임 카운트 통계
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CountDto(
    Integer all,           // 전체 게임 수
    Integer rated,         // 랭크 게임 수
    Integer win,           // 승리 수
    Integer loss,          // 패배 수
    Integer draw,          // 무승부 수
    Integer tour,          // 토너먼트 게임 수
    Integer berserk,       // 광폭 모드 사용 수
    Double opAvg,          // 상대방 평균 레이팅
    Integer seconds,       // 총 플레이 시간 (초)
    Integer disconnects    // 연결 끊김 수
) {
}

