package com.chessmate.external.lichess.dto.game;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GameStatus {
  CREATED("created"),
  STARTED("started"),
  ABORTED("aborted"),
  MATE("mate"),
  RESIGN("resign"),
  STALEMATE("stalemate"),
  TIMEOUT("timeout"),
  DRAW("draw"),
  OUTOFTIME("outoftime"),
  CHEAT("cheat"),
  NO_START("noStart"),
    UNKNOWN_FINISH("unknownFinish"),
  INSUFFICIENT_MATERIAL_CLAIM("insufficientMaterialClaim"),
  VARIANT_END("variantEnd"),
  UNKNOWN("unknown");

  private final String value;

}
