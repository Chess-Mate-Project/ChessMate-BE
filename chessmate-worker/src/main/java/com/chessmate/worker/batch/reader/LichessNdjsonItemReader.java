package com.chessmate.worker.batch.reader;

import com.chessmate.external.lichess.dto.game.LichessGamesDto;
import com.chessmate.external.lichess.LichessApiService;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Slf4j
@RequiredArgsConstructor
public class LichessNdjsonItemReader implements ItemReader<LichessGamesDto> {

  private final LichessApiService lichessApiService;

  @Value("#{jobParameters['token']}")
  private String token;

  @Value("#{jobParameters['username']}")
  private String username;

  @Value("#{jobParameters['since']}")
  private Long since;

  @Value("#{jobParameters['until']}")
  private Long until;

  @Value("#{jobParameters['batchId']}")
  private String batchId;

  private Iterator<LichessGamesDto> iterator;
  private int gameCount = 0;

  @Override
  public LichessGamesDto read() {
    if (iterator == null) {
      log.info("[Batch-Reader] [START] 게임 데이터 조회 시작 - batchId={}, username={}, since={}, until={}",
          batchId, username, since, until);
      iterator = lichessApiService
          .getUserGamesReactive(token, username, since, until)
          .toIterable()
          .iterator();
      log.info("[Batch-Reader] [STREAM-OPEN] 게임 스트림 오픈 완료 - batchId={}, username={}",
          batchId, username);
    }

    if (iterator.hasNext()) {
      LichessGamesDto game = iterator.next();
      gameCount++;
      log.debug("[Batch-Reader] [GAME-READ] gameCount={}, gameId={}, createdAt={}, lastMoveAt={}",
          gameCount, game.id(), game.createdAt(), game.lastMoveAt());
      return game;
    } else {
      log.info("[Batch-Reader] [COMPLETE] 게임 조회 완료 - batchId={}, username={}, totalCount={}",
          batchId, username, gameCount);
      return null;
    }
  }
}
