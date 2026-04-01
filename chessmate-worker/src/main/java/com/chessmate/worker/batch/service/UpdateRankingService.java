package com.chessmate.worker.batch.service;

import com.chessmate.common.type.GameType;
import com.chessmate.infra_redis.redis.CacheService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateRankingService {

  private final UserPerfRepository userPerfRepository;
  private final CacheService cacheService;

  public void buildAndCacheSnapshots(String batchId) {
    log.info("[RankingSnapshot] start batchId={}", batchId);

    for (GameType gameType : GameType.values()) {
      List<UserPerf> ranking = userPerfRepository.findRankingByGameType(gameType);
      cacheService.saveRanking(gameType, ranking);
      log.info("[RankingSnapshot] cached gameType={}, size={}, batchId={}", gameType, ranking.size(), batchId);
    }

    log.info("[RankingSnapshot] done batchId={}", batchId);
  }
}