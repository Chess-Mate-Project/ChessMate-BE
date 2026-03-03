package com.chessmate.api.lichess.stat.dto;

import com.chessmate.common.type.GameType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class ColorStatsResponse {
  private GameType gameType;
  private int whiteTotal;
  private int whiteWins;
  private int whiteLoses;
  private int whiteDraws;
  private int blackTotal;
  private int blackWins;
  private int blackLoses;
  private int blackDraws;
}
