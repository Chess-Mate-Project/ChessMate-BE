package com.chessmate.api.rank.service;

import com.chessmate.api.image.ImageUtil;
import com.chessmate.api.rank.dto.MyRankInfo;
import com.chessmate.api.rank.dto.RankerDto;
import com.chessmate.api.rank.dto.RankingResponse;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.user.User;
import com.chessmate.domain.userPerf.UserPerf;
import com.chessmate.domain.userPerf.UserPerfRepository;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.infra_redis.redis.CacheService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

  private final UserPerfRepository userPerfRepository;
  private final UserRepositoryImpl userRepository;
  private final CacheService cacheService;
  private final ImageUtil imageUtil;

  @Transactional(readOnly = true)
  public RankingResponse getRankers(User user, GameType gameType, Pageable pageable) {
    log.info("[Ranking Request] gameType={}, page={}, pageSize={}, user={}",
        gameType, pageable.getPageNumber(), pageable.getPageSize(), user != null ? user.getUsername() : "Anonymous");

    RankingResponse rankingResponse = new RankingResponse();

    // 비로그인 유저 처리
    if (user == null) {
      log.info("[Ranking] 비로그인 사용자 - 게스트 랭킹 제공 gameType={}, page={}", gameType, pageable.getPageNumber());
      rankingResponse.setMyRankInfo(MyRankInfo.imNotLoginUser());
      rankingResponse.setRanking(new ArrayList<>());
      rankingResponse.setTotalCount(0);
      rankingResponse.setCurrentPage(pageable.getPageNumber());
      rankingResponse.setPageSize(pageable.getPageSize());
      rankingResponse.setTotalPages(0);
      return rankingResponse;
    }

    // Cache-Aside Pattern: 캐시에서 조회
    List<UserPerf> allRankings = cacheService.getRanking(gameType);

    if (allRankings != null && !allRankings.isEmpty()) {
      log.info("[Cache-Hit] Ranking 캐시 조회 성공 - gameType={}", gameType);
    } else {
      log.info("[Cache-Miss] Ranking 캐시 미스, DB 조회 시작 - gameType={}", gameType);

      // DB에서 전체 랭킹 조회
      allRankings = userPerfRepository.findRankingByGameType(gameType);
      if (allRankings == null) {
        allRankings = new ArrayList<>();
      }
      log.info("[DB-Query] 전체 랭킹 조회 완료 - gameType={}, totalCount={}", gameType, allRankings.size());
    }


    // 유저 Perf 조회
    UserPerf userPerf = userPerfRepository.findByUserIdAndGameType(user.getId(), gameType)
        .orElseThrow(() -> new IllegalArgumentException("유저 퍼포먼스 정보를 찾을 수 없습니다."));

    User saveUser = userRepository.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다. - 랭킹 부문"));


    // 언레이팅 유저 감지하기
    MyRankInfo myRankInfo;
    if (userPerf.getRated() < 50) {
      myRankInfo = MyRankInfo.builder()
          .loggedInUser(true)
          .unrated(true)
          .userId(saveUser.getId())
          .username(saveUser.getUsername())
          .description(saveUser.getDescription())
          .banner(imageUtil.getBannerImageUrl(saveUser))
          .profile(imageUtil.getProfileImageUrl(saveUser))
          .rating(userPerf.getRating())
          .rank(0) // rank 0 = 언레이팅 유저
          .build();
    } else {
      int myRank = (int) allRankings.stream()
          .takeWhile(r -> r.getRating() > userPerf.getRating())
          .count() + 1;

      myRankInfo = MyRankInfo.builder()
          .loggedInUser(true)
          .unrated(false)
          .userId(saveUser.getId())
          .username(saveUser.getUsername())
          .description(saveUser.getDescription())
          .banner(imageUtil.getBannerImageUrl(saveUser))
          .profile(imageUtil.getProfileImageUrl(saveUser))
          .rating(userPerf.getRating())
          .rank(myRank)
          .build();
    }

    rankingResponse.setMyRankInfo(myRankInfo);



    // 페이지네이션 계산 (Pageable 사용)
    int totalCount = allRankings.size();
    int pageSize = pageable.getPageSize();
    int currentPage = pageable.getPageNumber();
    long totalPages = (long) Math.ceil((double) totalCount / pageSize);
    int startIndex = (int) pageable.getOffset();
    int endIndex = Math.min(startIndex + pageSize, totalCount);

    log.info("[Pagination] totalCount={}, pageSize={}, currentPage={}, totalPages={}, startIndex={}, endIndex={}",
        totalCount, pageSize, currentPage, totalPages, startIndex, endIndex);

    // 페이지 범위 검증
    if (startIndex >= totalCount && totalCount > 0) {
      log.warn("[Pagination] 잘못된 페이지 요청 - page={}, totalCount={}, startIndex={}",
          currentPage, totalCount, startIndex);
      rankingResponse.setRanking(new ArrayList<>());
      rankingResponse.setTotalCount(totalCount);
      rankingResponse.setCurrentPage(currentPage);
      rankingResponse.setPageSize(pageSize);
      rankingResponse.setTotalPages(totalPages);
      return rankingResponse;
    }

    // 페이지 데이터 추출
    List<RankerDto> rankers = new ArrayList<>();
    for (int i = startIndex; i < endIndex; i++) {
      UserPerf ranking = allRankings.get(i);
      var rankingUser = userRepository.findById(ranking.getUserId()).orElse(null);

      if (rankingUser != null) {
        RankerDto ranker = RankerDto.builder()
            .userId(ranking.getUserId())
            .username(rankingUser.getUsername())
            .rating(ranking.getRating())
            .rank(i + 1)
            .description(rankingUser.getDescription())
            .bannerImage(imageUtil.getBannerImageUrl(rankingUser))
            .profileImage(imageUtil.getProfileImageUrl(rankingUser))
            .build();
        rankers.add(ranker);
        log.debug("[Ranker] rank={}, username={}, rating={}", i + 1, rankingUser.getUsername(), ranking.getRating());
      }
    }

    rankingResponse.setRanking(rankers);
    rankingResponse.setTotalCount(totalCount);
    rankingResponse.setCurrentPage(currentPage);
    rankingResponse.setPageSize(pageSize);
    rankingResponse.setTotalPages(totalPages);

    log.info("[Ranking Complete] gameType={}, page={}, rankerCount={}, totalPages={}, responseTime={}ms",
        gameType, currentPage, rankers.size(), totalPages, 0);

    return rankingResponse;
  }
}







