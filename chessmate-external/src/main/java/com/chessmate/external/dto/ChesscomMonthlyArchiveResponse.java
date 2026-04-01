package com.chessmate.external.dto;

import com.chessmate.external.dto.chesscom.ChesscomGameResponse;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Chess.com 월간 게임 아카이브 응답 DTO
 * 사용자의 특정 연/월 게임 데이터 조회 시 반환되는 형식
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ChesscomMonthlyArchiveResponse {

    @JsonProperty("games")
    private List<ChesscomGameResponse> games;

}
