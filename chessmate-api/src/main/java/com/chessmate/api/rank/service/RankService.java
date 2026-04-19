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
    int pageSize = Math.max(pageable.getPageSize(), 1);
    int currentPage = pageable.getPageNumber();

    // DB 레벨 페이지네이션: 요청된 페이지만 조회
    long total = userPerfStatRepository.countByPlatformAndTimeClass(platform, timeClass);
    MyRankInfo myRankInfo = buildMyRankInfo(userId, userProvider, platform, timeClass);

    int startOffset = currentPage * pageSize;

    if (total == 0 || startOffset >= total) {
      return RankingResponse.builder()
          .myRankInfo(myRankInfo)
          .ranking(List.of())
          .totalCount(total)
          .currentPage(currentPage)
          .pageSize(pageSize)
          .totalPages((long) Math.ceil((double) total / pageSize))
          .build();
    }

    List<UserPerfStat> pageItems =
        userPerfStatRepository.findRankingPageByPlatformAndTimeClass(platform, timeClass, currentPage, pageSize);
    List<Long> pageUserIds = pageItems.stream().map(UserPerfStat::getUserId).toList();
    List<RankerDto> rankers = buildRankers(pageItems, pageUserIds, platform, startOffset);

    return RankingResponse.builder()
        .myRankInfo(myRankInfo)
        .ranking(rankers)
        .totalCount(total)
        .currentPage(currentPage)
        .pageSize(pageSize)
        .totalPages((long) Math.ceil((double) total / pageSize))
        .build();
  }

  private MyRankInfo buildMyRankInfo(Long userId, OAuthPlatForm userProvider,
                                     OAuthPlatForm platform, String timeClass) {
    if (userProvider != platform) {
      return buildUserInfoWithRank(userId, userProvider, 0, 0, true);
    }

    Optional<UserPerfStat> myPerfOpt =
        userPerfStatRepository.findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass);

    if (myPerfOpt.isEmpty()) {
      return buildUserInfoWithRank(userId, platform, 0, 0, false);
    }

    UserPerfStat myPerf = myPerfOpt.get();
    // 나보다 높은 순위(rating 높거나 동점 시 userId 작은) 인원 수 + 1 = 내 순위
    int myRank = (int) (userPerfStatRepository.countRankAbove(userId, platform, timeClass, myPerf.getRating()) + 1);

    return buildUserInfoWithRank(userId, platform, myRank, myPerf.getRating(), false);
  }

  private MyRankInfo buildUserInfoWithRank(Long userId, OAuthPlatForm platform,
                                           int rank, int rating, boolean platformMismatch) {
    return switch (platform) {
      case LICHESS -> {
        LichessUser u = lichessUserRepository.findById(userId).orElse(null);
        yield MyRankInfo.builder()
            .loggedInUser(true)
            .platformMismatch(platformMismatch)
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
            .platformMismatch(platformMismatch)
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