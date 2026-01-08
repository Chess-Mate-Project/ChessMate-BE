package com.chessmate.external.dto.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 단일 게임 타입 퍼포먼스 DTO
 *
 * - Lichess가 계산한 현재 레이팅 스냅샷
 * - 티어 계산 및 실력 판단의 핵심 데이터
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerfDto(

    /** 해당 게임 타입에서 둔 총 게임 수 */
    int games,

    /** 현재 레이팅 점수 */
    int rating,

    /** Rating Deviation (불확실성 지표, 낮을수록 신뢰도 높음) */
    int rd,

    /** 최근 레이팅 변화량 (증가/감소) */
    int prog,

    /**
     * 잠정 레이팅 여부
     * - true  : 게임 수 부족 → 신뢰도 낮음
     * - null  : 일반적으로 충분한 게임 수
     */
    Boolean prov
) {
}

