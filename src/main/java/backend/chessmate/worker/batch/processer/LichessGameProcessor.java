package backend.chessmate.worker.batch.processer;

import backend.chessmate.api.user.entity.User;
import backend.chessmate.api.user.entity.UserColorStat;
import backend.chessmate.api.user.entity.UserDailyStreak;
import backend.chessmate.api.user.entity.UserFirstMoveStat;
import backend.chessmate.api.user.entity.type.ChessColor;
import backend.chessmate.api.user.entity.type.GameResult;
import backend.chessmate.api.user.repository.UserRepository;
import backend.chessmate.global.external.dto.game.LichessGamesDto;
import backend.chessmate.worker.batch.dto.GameStat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@StepScope
public class LichessGameProcessor
    implements ItemProcessor<LichessGamesDto, GameStat> {

  private final UserRepository userRepository;

  @Value("#{jobParameters['username']}")
  private String username;

  @Override
  public GameStat process(LichessGamesDto game) {
    if (game.winner() == null) {
      return null; // 무승부 게임은 통계에 포함 ㄴㄴ
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

    //UserColorStat - 사용자 흑  /  백 승률 계산
    UserColorStat userColorStat = getColorStat(game, user);

    //UserFirstMoveStat - 사용자 첫수 통계 계산
    UserFirstMoveStat userFirstMoveStat = getFirstMoveStat(game, user);

    //UserDailyStreak - 사용자 일일 스트릭 계산
    UserDailyStreak dailyStreak = getDailyStreak(game, user);

    return GameStat.builder()
        .colorStat(userColorStat)
        .firstMoveStat(userFirstMoveStat)
        .dailyStreak(dailyStreak)
        .build();
  }

  private UserDailyStreak getDailyStreak(LichessGamesDto game, User user) {
    LocalDate date = Instant.ofEpochMilli(game.createdAt())
        .atZone(ZoneId.systemDefault())
        .toLocalDate();

    int win = 0, lose = 0, draw = 0;
    GameResult result = resolveMyGameResult(game, resolveMyColor(game, user));
    switch (result) {
      case WIN -> win = 1;
      case LOSE -> lose = 1;
      case DRAW -> draw = 1;
    }

    return UserDailyStreak.builder()
        .userId(user.getId())
        .date(date)
        .win(win)
        .lose(lose)
        .draw(draw)
        .lastGameAt(game.lastMoveAt())
        .build();
  }
  
  private UserColorStat getColorStat(LichessGamesDto game, User user) {
    ChessColor userColor = resolveMyColor(game, user);
    GameResult userGameResult = resolveMyGameResult(game, userColor);

    return UserColorStat.builder()
        .color(userColor)
        .userId(user.getId())
        .result(userGameResult)
        .build();

  }
  
  private UserFirstMoveStat getFirstMoveStat(LichessGamesDto game, User user) {
    ChessColor userColor = resolveMyColor(game, user);
    
    if (game.moves() == null || game.moves().isEmpty()) {
      throw new IllegalArgumentException("에러 발생 해당 게임 오류");
    }
    List<String> firstMove = List.of(game.moves().split(" "));

    UserFirstMoveStat stat = UserFirstMoveStat.builder()
        .userId(user.getId())
        .build();
        
    if (ChessColor.BLACK == userColor) {
      stat.setFirstMove(firstMove.get(0));
    } else if (ChessColor.WHITE == userColor) {
      stat.setFirstMove(firstMove.get(1));
    } else {
      throw new IllegalArgumentException("에러 발생 해당 게임 오류");
    }

    return stat;
  }
  
  private ChessColor resolveMyColor(LichessGamesDto game, User user) {
    final String BLACK_ID = game.players().black().user().id();
    final String WHITE_ID = game.players().white().user().id();

    if (BLACK_ID.equals(user.getLichessId())) {
      return ChessColor.BLACK;
    } else if (WHITE_ID.equals(user.getLichessId())) {
      return ChessColor.WHITE;
    } else {
      throw new IllegalArgumentException("에러 발생 해당 게임 오류");
    }
  }


  private GameResult resolveMyGameResult(LichessGamesDto game, ChessColor userColor) {
    if (game.winner().equals("black")) {
      return userColor == ChessColor.BLACK ? GameResult.WIN : GameResult.LOSE;
    } else if (game.winner().equals("white")) {
      return userColor == ChessColor.WHITE ? GameResult.WIN : GameResult.LOSE;
    } else {
      if (game.status().equals("draw")) {
        return GameResult.DRAW;
      } else {
        throw new IllegalArgumentException("에러 발생 해당 게임 오류");
      }
    }
  }

}
