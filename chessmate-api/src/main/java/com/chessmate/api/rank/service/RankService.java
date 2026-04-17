package com.chessmate.api.rank.service;

import com.chessmate.api.image.ImageUtil;
import com.chessmate.api.rank.dto.MyRankInfo;
import com.chessmate.api.rank.dto.RankerDto;
import com.chessmate.api.rank.dto.RankingResponse;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.chesscom.user.ChesscomUser;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import com.chessmate.domain.stat.UserPerfStat;
import com.chessmate.domain.stat.UserPerfStatRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

  private final UserPerfStatRepository userPerfStatRepository;
  private final LichessUserRepository lichessUserRepository;
  private final ChesscomUserRepository chesscomUserRepository;
  private final ImageUtil imageUtil;

  @Transactional(readOnly = true)
  public RankingResponse getRankers(Long userId, OAuthPlatForm userProvider, OAuthPlatForm platform,
                                    GameType gameType, Pageable pageable) {
    if (userId == null || userProvider == null || platform == null) {
      return buildGuestResponse(pageable);
    }

    String timeClass = gameType.toTimeClass();

    // 전체 랭킹 DB 직접 조회 (rating DESC, userId ASC)
    List<UserPerfStat> allRankings =
        userPerfStatRepository.findRankingByPlatformAndTimeClass(platform, timeClass);

    // 로그인한 플랫폼과 조회 플랫폼이 다르면 내 랭킹 없음
    Optional<UserPerfStat> myPerfOpt = userProvider == platform
        ? userPerfStatRepository.findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass)
        : Optional.empty();

    // 내 순위 정보 구성
    MyRankInfo myRankInfo = buildMyRankInfo(userId, userProvider, platform, myPerfOpt, allRankings);

    // 페이지네이션
    int total = allRankings.size();
    int pageSize = pageable.getPageSize();
    int currentPage = pageable.getPageNumber();
    int startIndex = (int) pageable.getOffset();
    int endIndex = Math.min(startIndex + pageSize, total);

    if (total == 0 || startIndex >= total) {
      return RankingResponse.builder()
          .myRankInfo(myRankInfo)
          .ranking(List.of())
          .totalCount(total)
          .currentPage(currentPage)
          .pageSize(pageSize)
          .totalPages((long) Math.ceil((double) total / pageSize))
          .build();
    }

    // 페이지 내 유저 정보 bulk 조회 (N+1 방지)
    List<UserPerfStat> pageItems = allRankings.subList(startIndex, endIndex);
    List<Long> pageUserIds = pageItems.stream().map(UserPerfStat::getUserId).toList();
    List<RankerDto> rankers = buildRankers(pageItems, pageUserIds, platform, startIndex);

    return RankingResponse.builder()
        .myRankInfo(myRankInfo)
        .ranking(rankers)
        .totalCount(total)
        .currentPage(currentPage)
        .pageSize(pageSize)
        .totalPages((long) Math.ceil((double) total / pageSize))
        .build();
  }

  private MyRankInfo buildMyRankInfo(Long userId, OAuthPlatForm userProvider, OAuthPlatForm platform,
                                     Optional<UserPerfStat> myPerfOpt,
                                     List<UserPerfStat> allRankings) {
    if (userProvider != platform) {
      return MyRankInfo.notLoggedIn();
    }

    if (myPerfOpt.isEmpty()) {
      // 해당 gameType 게임 이력 없음 → rank 0
      return buildUserInfoWithRank(userId, platform, 0, 0);
    }

    UserPerfStat myPerf = myPerfOpt.get();

    // allRankings는 DB에서 rating DESC, userId ASC로 정렬되어 옴
    // 내 위치 = index + 1
    int myRank = 0;
    for (int i = 0; i < allRankings.size(); i++) {
      if (allRankings.get(i).getUserId().equals(userId)) {
        myRank = i + 1;
        break;
      }
    }

    return buildUserInfoWithRank(userId, platform, myRank, myPerf.getRating());
  }

  private MyRankInfo buildUserInfoWithRank(Long userId, OAuthPlatForm platform,
                                           int rank, int rating) {
    return switch (platform) {
      case LICHESS -> {
        LichessUser u = lichessUserRepository.findById(userId).orElse(null);
        yield MyRankInfo.builder()
            .loggedInUser(true)
            .rank(rank)
            .rating(rating)
            .userId(userId)
            .username(u != null ? u.getUsername() : "Unknown")
            .banner(imageUtil.getBannerImageUrl(userId, u != null ? u.getBanner() : null))
            .profile(imageUtil.getProfileImageUrl(userId, u != null ? u.getProfile() : null))
            .description(u != null ? u.getDescription() : null)
            .build();
      }
      case CHESSCOM -> {
        ChesscomUser u = chesscomUserRepository.findById(userId).orElse(null);
        yield MyRankInfo.builder()
            .loggedInUser(true)
            .rank(rank)
            .rating(rating)
            .userId(userId)
            .username(u != null ? u.getUsername() : "Unknown")
            .banner(imageUtil.getBannerImageUrl(userId, u != null ? u.getBanner() : null))
            .profile(imageUtil.getProfileImageUrl(userId, u != null ? u.getProfile() : null))
            .description(u != null ? u.getDescription() : null)
            .build();
      }
    };
  }

  private List<RankerDto> buildRankers(List<UserPerfStat> pageItems, List<Long> userIds,
                                       OAuthPlatForm platform, int startOffset) {
    return switch (platform) {
      case LICHESS -> buildLichessRankers(pageItems, userIds, startOffset);
      case CHESSCOM -> buildChesscomRankers(pageItems, userIds, startOffset);
    };
  }

  private List<RankerDto> buildLichessRankers(List<UserPerfStat> pageItems,
                                               List<Long> userIds, int startOffset) {
    Map<Long, LichessUser> userMap = lichessUserRepository.findByIdIn(userIds)
        .stream().collect(Collectors.toMap(LichessUser::getId, u -> u));

    List<RankerDto> rankers = new ArrayList<>();
    for (int i = 0; i < pageItems.size(); i++) {
      UserPerfStat stat = pageItems.get(i);
      LichessUser u = userMap.get(stat.getUserId());
      rankers.add(RankerDto.builder()
          .userId(stat.getUserId())
          .username(u != null ? u.getUsername() : "Unknown")
          .rating(stat.getRating())
          .rank(startOffset + i + 1)
          .bannerImage(imageUtil.getBannerImageUrl(stat.getUserId(), u != null ? u.getBanner() : null))
          .profileImage(imageUtil.getProfileImageUrl(stat.getUserId(), u != null ? u.getProfile() : null))
          .description(u != null ? u.getDescription() : null)
          .build());
    }
    return rankers;
  }

  private List<RankerDto> buildChesscomRankers(List<UserPerfStat> pageItems,
                                                List<Long> userIds, int startOffset) {
    Map<Long, ChesscomUser> userMap = chesscomUserRepository.findByIdIn(userIds)
        .stream().collect(Collectors.toMap(ChesscomUser::getId, u -> u));

    List<RankerDto> rankers = new ArrayList<>();
    for (int i = 0; i < pageItems.size(); i++) {
      UserPerfStat stat = pageItems.get(i);
      ChesscomUser u = userMap.get(stat.getUserId());
      rankers.add(RankerDto.builder()
          .userId(stat.getUserId())
          .username(u != null ? u.getUsername() : "Unknown")
          .rating(stat.getRating())
          .rank(startOffset + i + 1)
          .bannerImage(imageUtil.getBannerImageUrl(stat.getUserId(), u != null ? u.getBanner() : null))
          .profileImage(imageUtil.getProfileImageUrl(stat.getUserId(), u != null ? u.getProfile() : null))
          .description(u != null ? u.getDescription() : null)
          .build());
    }
    return rankers;
  }

  private RankingResponse buildGuestResponse(Pageable pageable) {
    return RankingResponse.builder()
        .myRankInfo(MyRankInfo.notLoggedIn())
        .ranking(List.of())
        .totalCount(0)
        .currentPage(pageable.getPageNumber())
        .pageSize(pageable.getPageSize())
        .totalPages(0)
        .build();
  }
}