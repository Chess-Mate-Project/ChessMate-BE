package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChesscomRatingEntry(
    int rating
) {}
