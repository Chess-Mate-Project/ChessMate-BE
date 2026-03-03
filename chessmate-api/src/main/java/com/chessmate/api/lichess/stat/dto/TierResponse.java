package com.chessmate.api.lichess.stat.dto;

import com.chessmate.common.dto.TierResult;
import com.chessmate.common.type.GameType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TierResponse {

  private GameType gameType;
  private int rating;
  private TierResult tierResult;
}
