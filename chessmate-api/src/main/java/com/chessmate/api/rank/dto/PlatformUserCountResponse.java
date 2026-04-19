package com.chessmate.api.rank.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlatformUserCountResponse {

  private int lichessCount;
  private int chesscomCount;
  private int totalCount;

}