package com.chessmate.external.dto.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Player(
    LichessUser user,
    Integer rating,
    @JsonProperty("ratingDiff") Integer ratingDiff
) {}
