package backend.chessmate.api.external.dto.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lichess 사용자 요약 DTO
 *
 * - /api/user/{username} 응답을 기반으로 함
 * - 서비스 내부에서 "유저 대표 정보"로 사용
 * - 퍼포먼스, 플레이타임, 전체 게임 통계를 포함
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LichessAccountDto(

    /** Lichess 내부 고유 사용자 ID (username과 다름, 불변) */
    String id,

    /** 사용자 닉네임 */
    String username,

//    /** Lichess 후원자(patron) 여부 */
//    boolean patron,

    /** 계정 생성 시각 (epoch millis) */
    long createdAt,

    /** 마지막 접속 시각 (epoch millis) */
    long seenAt,

    /** 전체 플레이 시간 정보 */
    PlayTimeDto playTime,

    /** 전체 게임 수 / 승패무 통계 */
    UserCountDto count,

    /** 게임 타입별 퍼포먼스(레이팅) 정보 */
    PerfsDto perfs
) {
}

