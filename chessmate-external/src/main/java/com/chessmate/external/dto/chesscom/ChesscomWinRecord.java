package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChesscomWinRecord(
    int win,
    int loss,
    int draw
) {}
