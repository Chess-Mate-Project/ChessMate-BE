package com.chessmate.api.lichess.stat.dto;

import com.chessmate.common.type.GameType;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FirstMoveResponse {
  private GameType gameType;
  private Map<String, Integer> whiteMoves;
  private Map<String, Integer> blackMoves;
}
