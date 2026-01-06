// 파일: `src/main/java/backend/chessmate/global/external/dto/game/LichessUser.java`
package backend.chessmate.global.external.dto.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LichessUser (
  String name,
  String flair,
  String title, //없을 수 있음
  String id
){}
