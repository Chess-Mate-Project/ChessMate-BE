
package com.chessmate.worker.batch.processer;


import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameResult;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.user.User;
import com.chessmate.domain.userColorStat.UserColorStat;
import com.chessmate.domain.userDailyStreak.UserDailyStreak;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.external.dto.game.LichessGamesDto;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.worker.batch.dto.GameStat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@StepScope
@Slf4j
public class LichessGameProcessor
    implements ItemProcessor<LichessGamesDto, GameStat> {

  private final UserRepositoryImpl userRepository;

  @Value("#{jobParameters['username']}")
  private String username;

  @Override
  public GameStat process(LichessGamesDto game) {

    log.info("게임 처리 시작: username='{}', game='{}'", username, game);
    if (game == null) {
      log.warn("처리중인 game이 null 입니다. username='{}' - 이 게임은 건너뜁니다.", username);
      return null;
    }
    if (game.players() == null) {
      log.warn("game.players가 null 입니다. username='{}', game='{}' - 이 게임은 건너뜁니다.", username, game);
      return null;
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> {
          log.error("사용자 조회 실패: username='{}' - 게임 처리 불가, game='{}'", username, game);
          return new IllegalArgumentException("User not found: " + username);
        });

    ChessColor myColor = resolveMyColor(game, user);
    if (myColor == null) {
      log.info("해당 게임이 사용자 게임이 아님 또는 익명/봇입니다. username='{}', game='{}' - 건너뜁니다.", username, game);
      return null; // 내 게임 아님 / 익명 / 봇
    }

    GameResult result = resolveMyGameResult(game, myColor);
    if (result == null) {
      log.warn("게임 결과 판별 실패: username='{}', game='{}' - 건너뜁니다.", username, game);
      return null;
    }

    return GameStat.builder()
        .colorStat(buildColorStat(user, myColor, result, getGameType(game.perf())))
        .firstMoveStat(buildFirstMoveStat(game, user, myColor))
        .dailyStreak(buildDailyStreak(game, user, result))
        .build();
  }

  // ==============================
  // Color
  // ==============================
  private ChessColor resolveMyColor(LichessGamesDto game, User user) {

    var black = game.players().black();
    var white = game.players().white();

    String blackUserId = null;
    String whiteUserId = null;

    if (black == null) {
      log.debug("black 플레이어 정보 없음: username='{}', game.createdAt='{}'", username, game.createdAt());
    } else {
      if (black.user() == null) {
        log.debug("black.user가 null 입니다: username='{}', game.createdAt='{}'", username,
            game.createdAt());
      } else {
        blackUserId = black.user().id();
        if (user.getLichessId().equals(blackUserId)) {
          log.debug("내 색상: BLACK 확인됨. username='{}', lichessId='{}', game.createdAt='{}'", username,
              blackUserId, game.createdAt());
          return ChessColor.BLACK;
        }
      }
    }

    if (white == null) {
      log.debug("white 플레이어 정보 없음: username='{}', game.createdAt='{}'", username, game.createdAt());
    } else {
      if (white.user() == null) {
        log.debug("white.user가 null 입니다: username='{}', game.createdAt='{}'", username,
            game.createdAt());
      } else {
        whiteUserId = white.user().id();
        if (user.getLichessId().equals(whiteUserId)) {
          log.debug("내 색상: WHITE 확인됨. username='{}', lichessId='{}', game.createdAt='{}'", username,
              whiteUserId, game.createdAt());
          return ChessColor.WHITE;
        }
      }
    }

    log.debug(
        "사용자와 일치하는 플레이어 ID 없음: username='{}', userLichessId='{}', blackId='{}', whiteId='{}', game.createdAt='{}'",
        username, user.getLichessId(), blackUserId, whiteUserId, game.createdAt());
    return null;
  }

  // ==============================
  // Result
  // ==============================
  private GameResult resolveMyGameResult(LichessGamesDto game, ChessColor myColor) {

    if (game.winner() == null) {
      log.info("winner가 null 이므로 무승부로 처리합니다. username='{}', game.createdAt='{}'", username,
          game.createdAt());
      return null;
    }

    return switch (game.winner()) {
      case "black" -> {
        GameResult r = (myColor == ChessColor.BLACK ? GameResult.WIN : GameResult.LOSE);
        log.debug("winner='black' -> username='{}', myColor='{}', result='{}', game.createdAt='{}'",
            username, myColor, r, game.createdAt());

        yield r;
      }
      case "white" -> {
        GameResult r = (myColor == ChessColor.WHITE ? GameResult.WIN : GameResult.LOSE);
        log.debug("winner='white' -> username='{}', myColor='{}', result='{}', game.createdAt='{}'",
            username, myColor, r, game.createdAt());

        yield r;
      }
      default -> {
        log.warn("예상치 못한 winner 값: '{}'. 무승부로 처리합니다. username='{}', game.createdAt='{}'",
            game.winner(), username, game.createdAt());
        yield null;
      }
    };
  }

  // ==============================
  // Stats
  // ==============================
  private UserColorStat buildColorStat(
      User user,
      ChessColor color,
      GameResult result,
      GameType gameType
  ) {
    return UserColorStat.builder()
        .userId(user.getId())
        .color(color)
        .result(result)
        .gameType(gameType)
        .build();
  }

  private UserFirstMoveStat buildFirstMoveStat(
      LichessGamesDto game,
      User user,
      ChessColor color
  ) {

    if (game.moves() == null) {
      log.info("moves가 null 입니다. 첫 수 통계는 생성하지 않습니다. username='{}', game.createdAt='{}'", username,
          game.createdAt());
      return null;
    }

    String[] moves = game.moves().split(" ");
    if (moves.length < 2) {
      log.info("수의 개수가 충분하지 않습니다. length={}, username='{}', game.createdAt='{}' - 첫 수 통계 생략",
          moves.length, username, game.createdAt());
      return null;
    }

    String firstMove =
        color == ChessColor.WHITE ? moves[0] : moves[1];

    log.debug("첫 수 기록: username='{}', color='{}', firstMove='{}', game.createdAt='{}'", username,
        color, firstMove, game.createdAt());

    return UserFirstMoveStat.builder()
        .userId(user.getId())
        .firstMove(firstMove)
        .gameType(getGameType(game.perf()))
        .build();
  }

  private UserDailyStreak buildDailyStreak(
      LichessGamesDto game,
      User user,
      GameResult result
  ) {

    LocalDate date = Instant.ofEpochMilli(game.createdAt())
        .atZone(ZoneId.systemDefault())
        .toLocalDate();

    log.debug("daily streak 빌드: username='{}', date='{}', result='{}', lastMoveAt='{}'", username,
        date, result, game.lastMoveAt());

    return UserDailyStreak.builder()
        .userId(user.getId())
        .date(date)
        .win(result == GameResult.WIN ? 1 : 0)
        .lose(result == GameResult.LOSE ? 1 : 0)
        .draw(result == GameResult.DRAW ? 1 : 0)
        .lastGameAt(game.lastMoveAt())
        .build();
  }

  public GameType getGameType(String perf) {
    return switch (perf) {
      case "bullet" -> GameType.BULLET;
      case "blitz" -> GameType.BLITZ;
      case "rapid" -> GameType.RAPID;
      case "classical" -> GameType.CLASSICAL;
      default -> null;
    };

  }
}
