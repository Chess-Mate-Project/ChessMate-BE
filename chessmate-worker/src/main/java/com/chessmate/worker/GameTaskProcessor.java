package com.chessmate.worker;

import com.chessmate.common.dto.GameSyncTaskDto;
import com.chessmate.external.api.chesscom.ChesscomApi;
import com.chessmate.external.api.lichess.LichessApi;
import com.chessmate.external.dto.chesscom.ChesscomGameArchivesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameTaskProcessor {

  private final ChesscomApi chesscomApi;
  private final LichessApi lichessApi;


  public void process(GameSyncTaskDto task) {
    if (task.getPlatform().equals("lichess")) {
      lichessGameProcess(task);
    } else if (task.getPlatform().equals("chesscom")) {
      chesscomGameProcess(task);
    }
  }

  private void lichessGameProcess(GameSyncTaskDto task) {

  }

  private void chesscomGameProcess(GameSyncTaskDto task) {
    ChesscomGameArchivesResponse data = chesscomApi.getGameArchives(task.getPlatformUsername());

    data.getArchives().stream().forEach(archive -> {
      chesscomApi.getArchiveByUrl(archive);
    });
  }
}
