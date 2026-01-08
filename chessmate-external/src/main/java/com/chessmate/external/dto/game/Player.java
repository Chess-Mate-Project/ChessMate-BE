package com.chessmate.external.dto.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Player(LichessUser user, Integer rating, Integer ratingDiff) {}
