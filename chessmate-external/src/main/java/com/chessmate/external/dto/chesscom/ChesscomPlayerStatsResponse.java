package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChesscomPlayerStatsResponse(

    @JsonProperty("chess_bullet")
    ChesscomTimeClassStat chessBullet,

    @JsonProperty("chess_blitz")
    ChesscomTimeClassStat chessBlitz,

    @JsonProperty("chess_rapid")
    ChesscomTimeClassStat chessRapid
) {}
