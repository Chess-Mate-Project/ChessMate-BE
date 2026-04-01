package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Chess.com 게임 플레이어 정보 DTO
 * 게임의 백/흑 플레이어 정보를 담음
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ChesscomGamePlayerInfo {

    @JsonProperty("username")
    private String username;

    @JsonProperty("rating")
    private Integer rating;

    @JsonProperty("result")
    private String result;

    @JsonProperty("@id")
    private String id;

    @JsonProperty("uuid")
    private String uuid;
}

