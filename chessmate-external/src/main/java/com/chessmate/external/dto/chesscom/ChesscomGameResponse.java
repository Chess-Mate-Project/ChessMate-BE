package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Chess.com 게임 데이터 응답 DTO
 * Chess.com PubAPI의 월간 아카이브 엔드포인트에서 반환되는 게임 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ChesscomGameResponse {

    @JsonProperty("url")
    private String url;

    @JsonProperty("pgn")
    private String pgn;

    @JsonProperty("time_control")
    private String timeControl;

    @JsonProperty("end_time")
    private Long endTime;

    @JsonProperty("rated")
    private Boolean rated;

    @JsonProperty("accuracies")
    private Map<String, Double> accuracies;

    @JsonProperty("fen")
    private String fen;

    @JsonProperty("start_time")
    private Long startTime;

    @JsonProperty("time_class")
    private String timeClass;

    @JsonProperty("rules")
    private String rules;

    @JsonProperty("white")
    private ChesscomGamePlayerInfo white;

    @JsonProperty("black")
    private ChesscomGamePlayerInfo black;

    @JsonProperty("eco")
    private String eco;

    @JsonProperty("tournament")
    private String tournament;

    @JsonProperty("match")
    private String match;
}
