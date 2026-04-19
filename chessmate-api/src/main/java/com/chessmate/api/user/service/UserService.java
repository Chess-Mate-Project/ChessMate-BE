package com.chessmate.api.user.service;

import static com.chessmate.common.dto.OAuthPlatForm.CHESSCOM;
import static com.chessmate.common.dto.OAuthPlatForm.LICHESS;

import com.chessmate.api.image.ImageUtil;
import com.chessmate.api.rank.dto.PlatformUserCountResponse;
import com.chessmate.api.user.dto.ProfileResponse;
import com.chessmate.api.user.dto.SearchUsersResponse;
import com.chessmate.api.user.dto.UserSearchProfileResponse;
import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.exception.UserException;
import com.chessmate.domain.chesscom.user.ChesscomUser;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import com.chessmate.domain.stat.UserPerfStat;
import com.chessmate.domain.stat.UserPerfStatRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final LichessUserRepository lichessUserRepository;
    private final UserPerfStatRepository userPerfStatRepository;
    private final ChesscomUserRepository chesscomUserRepository;
    private final ImageUtil imageUtil;

  @Transactional(readOnly = true)
  public SearchUsersResponse searchUsers(String keyword, OAuthPlatForm platform, Long excludeUserId) {
    return switch (platform) {
      case LICHESS -> {
        List<LichessUser> lichessUsers = lichessUserRepository.searchByUsernameContaining(keyword);
        List<Long> userIds = lichessUsers.stream().map(LichessUser::getId).toList();
        Map<Long, UserPerfStat> statMap =
            userPerfStatRepository.findTopRatingByUserIdsAndPlatform(userIds, LICHESS);

        List<UserSearchProfileResponse> users = lichessUsers.stream()
            .filter(user -> !user.getId().equals(excludeUserId))
            .map(user -> {
              UserPerfStat top = statMap.get(user.getId());
              return UserSearchProfileResponse.builder()
                  .id(user.getId())
                  .rating(top != null ? top.getRating() : 0)
                  .profileImageUrl(imageUtil.getProfileImageUrl(user.getId(), user.getProfile()))
                  .platform(LICHESS)
                  .username(user.getUsername())
                  .build();
            })
            .toList();

        yield new SearchUsersResponse(users);
      }

      case CHESSCOM -> {
        List<ChesscomUser> chesscomUsers = chesscomUserRepository.searchByUsernameContaining(keyword);
        List<Long> userIds = chesscomUsers.stream().map(ChesscomUser::getId).toList();
        Map<Long, UserPerfStat> statMap =
            userPerfStatRepository.findTopRatingByUserIdsAndPlatform(userIds, CHESSCOM);

        List<UserSearchProfileResponse> users = chesscomUsers.stream()
            .filter(user -> !user.getId().equals(excludeUserId))
            .map(user -> {
              UserPerfStat top = statMap.get(user.getId());
              return UserSearchProfileResponse.builder()
                  .id(user.getId())
                  .rating(top != null ? top.getRating() : 0)
                  .profileImageUrl(imageUtil.getProfileImageUrl(user.getId(), user.getProfile()))
                  .platform(CHESSCOM)
                  .username(user.getUsername())
                  .build();
            })
            .toList();

        yield new SearchUsersResponse(users);
      }
    };
  }



    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId, OAuthPlatForm platform) {
        return switch (platform) {
            case LICHESS -> {
                LichessUser user = lichessUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
                yield new ProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    LICHESS,
                    user.getDescription(),
                    imageUtil.getProfileImageUrl(user.getId(), user.getProfile()),
                    imageUtil.getBannerImageUrl(user.getId(), user.getBanner()),
                    user.getCreatedAt(),
                    user.getPlatformJoinedAt()
                );
            }
            case CHESSCOM -> {
                ChesscomUser user = chesscomUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
                yield new ProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    CHESSCOM,
                    user.getDescription(),
                    imageUtil.getProfileImageUrl(user.getId(), user.getProfile()),
                    imageUtil.getBannerImageUrl(user.getId(), user.getBanner()),
                    user.getCreatedAt(),
                    user.getPlatformJoinedAt()
                );
            }
        };
    }

    @Transactional(readOnly = true)
    public PlatformUserCountResponse getPlatformUserCounts() {
        int lichessCount = lichessUserRepository.count();
        int chesscomCount = chesscomUserRepository.count();
        return PlatformUserCountResponse.builder()
            .lichessCount(lichessCount)
            .chesscomCount(chesscomCount)
            .totalCount(lichessCount + chesscomCount)
            .build();
    }

    @Transactional(readOnly = true)
    public Long resolveUserId(String username, OAuthPlatForm platform) {
        return switch (platform) {
            case LICHESS -> lichessUserRepository.findByUsername(username)
                .map(LichessUser::getId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
            case CHESSCOM -> chesscomUserRepository.findByUsername(username)
                .map(ChesscomUser::getId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
        };
    }

    @Transactional
    public void updateDescription(Long userId, OAuthPlatForm platform, String description) {
        switch (platform) {
            case LICHESS -> {
                LichessUser user = lichessUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
                user.setDescription(description);
                lichessUserRepository.save(user);
            }
            case CHESSCOM -> {
                ChesscomUser user = chesscomUserRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
                user.setDescription(description);
                chesscomUserRepository.save(user);
            }
        }
    }
}
