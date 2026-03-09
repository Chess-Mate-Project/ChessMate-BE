package com.chessmate.external.lichess.dto.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record LichessUser (
  String name,
  String flair,
  String title, //없을 수 있음
  String id
){}
