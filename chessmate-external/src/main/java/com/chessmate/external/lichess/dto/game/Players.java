package com.chessmate.external.lichess.dto.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Players(Player white, Player black) {}

