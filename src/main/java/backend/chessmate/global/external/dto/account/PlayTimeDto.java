package backend.chessmate.global.external.dto.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 사용자 플레이 시간 DTO
 *
 * - 전체 플레이 시간 및 Lichess TV 노출 시간
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayTimeDto(

    /** 전체 플레이 시간 (초 단위) */
    int total

//    /** Lichess TV에 노출된 시간 (초 단위) */
//    int tv
) {
}

