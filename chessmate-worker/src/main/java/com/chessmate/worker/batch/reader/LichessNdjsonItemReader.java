package com.chessmate.worker.batch.reader;

import com.chessmate.external.dto.game.LichessGamesDto;
import com.chessmate.external.service.LichessApiService;
import java.time.Instant;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class LichessNdjsonItemReader implements ItemReader<LichessGamesDto> {

  private final LichessApiService lichessApiService;

  @Value("#{jobParameters['token']}")
  private String token;

  @Value("#{jobParameters['username']}")
  private String username;

  @Value("#{jobParameters['since']}")
  private Long since;


  private Iterator<LichessGamesDto> iterator;

  @Override
  public LichessGamesDto read() {
    if (iterator == null) {
      iterator = lichessApiService
          .getUserGamesReactive(token, username, since)
          .toIterable()
          .iterator();
    }

    return iterator.hasNext() ? iterator.next() : null;
  }
}
