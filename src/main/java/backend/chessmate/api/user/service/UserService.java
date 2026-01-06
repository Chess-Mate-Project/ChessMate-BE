package backend.chessmate.api.user.service;

import backend.chessmate.api.user.dto.TotalUserCountResponse;
import backend.chessmate.api.user.dto.UpdateUserDescriptionRequest;
import backend.chessmate.api.user.entity.User;
import backend.chessmate.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public TotalUserCountResponse getTotalUserCount() {
    long totalUsers = userRepository.count();
    return new TotalUserCountResponse((int) totalUsers);
  }

  /**
   * - 사용자 자기소개 업데이트
   * - @param user 현재 사용자
   * - @param updateUserDescriptionRequest 사용자 자기소개 업데이트 요청 DTO
   * - @return void
   * */

  public void updateUserDescription(User user, UpdateUserDescriptionRequest updateUserDescriptionRequest) {
    userRepository.findById(user.getId()).ifPresent(u -> {
      u.setDescription(updateUserDescriptionRequest.description());
      userRepository.save(u);
    });
  }


//
//    private final StreakRepository streakRepository;
//    private final OpeningRepository openingRepository;
//    private final FirstMoveRepository firstMoveRepository;
//    private final UserRepository userRepository;
//    private final RedisService redisService;
//
//    public List<StreakDto> getStreak(User u, int year) {
//        List<Streak> streaks = streakRepository.findAllByUserAndYear(u, year);
//
//        return streaks.stream().map(s ->
//                new StreakDto(s.getDate(),
//                        s.getWin(),
//                        s.getLose(),
//                        s.getDraw()
//                )).toList();
//    }
//
//    public List<FirstMoveDto> getFirstMove(User u, int top) {
//        List<FirstMove> firstMoves = firstMoveRepository.findTopFirstMovesByUser(u, top);
//
//        return firstMoves.stream().map(fm ->
//                new FirstMoveDto(
//                        fm.getMove(),
//                        fm.getCount()
//                )).toList();
//    }
//
//    public List<OpeningDto> getOpening(User u, int top) {
//        List<Opening> openings = openingRepository.findTopOpeningsByUser(u, top);
//
//        return openings.stream().map(o ->
//                new OpeningDto(
//                        o.getOpening(),
//                        o.getCount()
//                )).toList();
//    }
//
//    public UserPlayCountDto getPlayCount(User u) {
//        String oauthKey = OAUTH_KEY + ":" + u.getId();
//        String oauthToken = redisService.get(oauthKey, String.class); // 유저 고유 lichess oauth api key
//
//        var key = PLAY_COUNT_KEY + ":" + u.getId(); // 레디스 저장 및 조회용 playCount Key
//
//        if (redisService.get(key, UserPlayCountDto.class) != null) { // 레디스에 playCount가 존재하면
//            return redisService.get(key, UserPlayCountDto.class); // 바로 꺼내서 반환
//        }
//
//        JsonNode userAccount = lichessUtil.getUserAccount(oauthToken);
//        UserPlayCountDto userPlayCountDto = JsonNodeUtil.mapToUserPlayCountDto(userAccount);  // lichess api (account) 조회
//        redisService.save(key, userPlayCountDto, 3600); // 1시간 레디스 저장 후
//        return userPlayCountDto; // 반환
//
//    }
//
//    public GameSummaryDto getGameSummary(User u) {
//        String oauthKey = OAUTH_KEY + ":" + u.getId();
//        String oauthToken = redisService.get(oauthKey, String.class); // 유저 고유 lichess oauth api key
//
//
//        var key = GAME_SUMMARY_KEY + ":" + u.getId(); // 레디스 저장 및 조회용 playCount Key
//
//        if (redisService.get(key, GameSummaryDto.class) != null) { // 레디스에 playCount가 존재하면
//            return redisService.get(key, GameSummaryDto.class); // 바로 꺼내서 반환
//        }
//
//        JsonNode userAccount = lichessUtil.getUserAccount(oauthToken);
//        GameSummaryDto gameSummaryDto = JsonNodeUtil.mapToGameSummaryDto(userAccount); // lichess api (account) 조회
//        redisService.save(key, gameSummaryDto, 3600); // 1시간 레디스 저장 후
//        return gameSummaryDto; // 반환
//
//    }
//
//    public TierInfoDto getTierInfo(User u) {
//        String oauthKey = OAUTH_KEY + ":" + u.getId();
//        String oauthToken = redisService.get(oauthKey, String.class); // 유저 고유 lichess oauth api key
//
//
//        var key = USER_TIERS_KEY + ":" + u.getId(); // 레디스 저장 및 조회용 playCount Key
//
//        if (redisService.get(key, TierInfoDto.class) != null) { // 레디스에 playCount가 존재하면
//            return redisService.get(key, TierInfoDto.class); // 바로 꺼내서 반환
//        }
//
//        JsonNode userAccount = lichessUtil.getUserAccount(oauthToken);
//        UserRatingByGameTypesMapper mapper = JsonNodeUtil.mapToUserRatingByGameTypesDto(userAccount); // lichess api (account) 조회
//
//        TierInfoDto tierinfoDto = TierInfoDto.builder()
//                .classical(TierUtil.calculateTier(mapper.getClassicalRating()))
//                .rapid(TierUtil.calculateTier(mapper.getRapidRating()))
//                .bullet(TierUtil.calculateTier(mapper.getBulletRating()))
//                .blitz(TierUtil.calculateTier(mapper.getBlitzRating()))
//                .build();
//
//
//        redisService.save(key, tierinfoDto, 3600); // 1시간 레디스 저장 후
//        return tierinfoDto; // 반환
//    }
//
//    public UserProfileDto getUserProfile(User u) {
//        String oauthKey = OAUTH_KEY + ":" + u.getId();
//        String oauthToken = redisService.get(oauthKey, String.class);
//
//        var key = USER_PROFILE_KEY + ":" + u.getId();
//
//
//        if (redisService.get(key, UserProfileMapper.class) != null) {
//            UserProfileMapper userProfileMapper = redisService.get(key, UserProfileMapper.class);
//
//            return UserProfileDto.builder()
//                    .name(u.getName())
//                    .createAt(u.getCreatedAt())
//                    .bio(userProfileMapper.getBio())
//                    .flag(userProfileMapper.getFlag())
//                    .link(userProfileMapper.getLink())
//                    .playTime(userProfileMapper.getPlayTime())
//                    .build();
//        }
//
//        JsonNode userAccount = lichessUtil.getUserAccount(oauthToken);
//        UserProfileMapper userProfileMapper = JsonNodeUtil.mapToUserProfileDto(userAccount);
//
//        redisService.save(key, userProfileMapper, 3600);
//
//
//        return UserProfileDto.builder()
//                .name(u.getName())
//                .createAt(u.getCreatedAt())
//                .bio(userProfileMapper.getBio())
//                .flag(userProfileMapper.getFlag())
//                .playTime(userProfileMapper.getPlayTime())
//                .build();
//    }
//
//    public UserTierHistoryDto getUserRatingHistory(User u, GameType gameType) {
//        var key = USER_RATING_HISTORY_KEY + ":" + u.getId();
//
//        UserRatingHistoryMapper mapper = null;
//        if (redisService.get(key, UserRatingHistoryMapper.class) != null) {
//            mapper = redisService.get(key, UserRatingHistoryMapper.class);
//        } else  {
//            JsonNode jsonNode = lichessUtil.getUserRatingHistoryApi(u);
//            mapper = JsonNodeUtil.mapToUserRatingHistoryByTierHistoryDto(jsonNode);
//            redisService.save(key, mapper, 604800); // 일주일
//        }
//
//        if (mapper == null) {
//            throw new RuntimeException("사용자 티어 변동 이력 조회 중 오류 발생 mapper = null");
//        }
//
//
//        UserTierHistoryDto userTierHistoryDto = UserTierHistoryDto.builder()
//                .gameType(gameType)
//                .build();
//
//        switch (gameType) {
//            case BULLET -> userTierHistoryDto.setHistory(mapper.getBulletHistory());
//            case BLITZ -> userTierHistoryDto.setHistory(mapper.getBlitzHistory());
//            case RAPID -> userTierHistoryDto.setHistory(mapper.getRapidHistory());
//            case CLASSICAL -> userTierHistoryDto.setHistory(mapper.getClassicalHistory());
//        }
//
//
//        return userTierHistoryDto;
//    }
//
}
