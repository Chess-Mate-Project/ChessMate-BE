package com.chessmate.external.lichess.dto.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 사용자 게임 수 통계 DTO
 *
 * - 전체 게임 및 승/패/무 요약 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserCountDto(

    /** 전체 게임 수 */
    int all,

    /** 레이티드 게임 수 */
    int rated,

    /** 승리 횟수 */
    int win,

    /** 패배 횟수 */
    int loss,

    /** 무승부 횟수 */
    int draw
) {
}
