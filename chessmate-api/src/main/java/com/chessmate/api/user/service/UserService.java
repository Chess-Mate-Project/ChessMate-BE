package com.chessmate.api.user.service;


import com.chessmate.api.image.ImageUtil;
import com.chessmate.api.user.dto.ProfileResponse;
import com.chessmate.api.user.dto.TotalUserCountResponse;
import com.chessmate.api.user.dto.UpdateUserDescriptionRequest;
import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.exception.UserException;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.infra_redis.redis.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepositoryImpl userRepository;
  private final CacheService cacheService;
  private final ImageUtil imageUtil;

  public TotalUserCountResponse getTotalUserCount() {
    long totalUsers = userRepository.count();
    return new TotalUserCountResponse((int) totalUsers);
  }

  /**
   * - 사용자 자기소개 업데이트 - @param user 현재 사용자 - @param updateUserDescriptionRequest 사용자 자기소개 업데이트 요청 DTO
   * - @return void
   *
   */

  public void updateUserDescription(User user,
      UpdateUserDescriptionRequest updateUserDescriptionRequest) {
    userRepository.findById(user.getId()).ifPresent(u -> {
      u.setDescription(updateUserDescriptionRequest.description());
      userRepository.save(u);

      // 캐시 무효화 - 프로필 정보가 변경되었으므로 캐시 삭제
      String cacheKey = buildProfileCacheKey(u.getId());
      cacheService.deleteCache(cacheKey);
      log.info("[Cache-Invalidate] UserProfile - userId={}", u.getId());
    });
  }

  /**
   * 사용자 프로필 조회 (Cache-Aside Pattern 적용)
   * - Cache-Hit: 캐시에서 즉시 반환
   * - Cache-Miss: DB 조회 후 캐시 저장
   */
  public ProfileResponse getUserProfile(User user) {
    log.info("[프로필 조회 시작] userId={}", user.getId());

    // Cache-Aside Pattern 1단계: 캐시에서 조회
    String cacheKey = buildProfileCacheKey(user.getId());
    ProfileResponse cachedData = cacheService.getCache(cacheKey, ProfileResponse.class);

    if (cachedData != null) {
      log.info("[Cache-Hit] UserProfile - userId={}, profileImageUrl={}, bannerImageUrl={}",
          user.getId(), cachedData.profileImage(), cachedData.bannerImage());
      return cachedData;
    }

    log.info("[Cache-Miss] UserProfile - userId={}, DB 조회 시작", user.getId());

    // Cache-Aside Pattern 2단계: 캐시 미스 시 DB에서 조회
    User u = userRepository.findById(user.getId()).orElseThrow(
        () -> new UserException(UserErrorCode.NOT_FOUND_USER)
    );

    log.debug("[DB 조회 완료] userId={}, profileImage={}, bannerImage={}",
        u.getId(), u.getProfileImage(), u.getBannerImage());

    // 이미지 URL 생성
    String profileImageUrl = imageUtil.getProfileImageUrl(u);
    String bannerImageUrl = imageUtil.getBannerImageUrl(u);

    log.info("[이미지 URL 생성 완료] userId={}, profileUrl={}, bannerUrl={}",
        u.getId(), profileImageUrl, bannerImageUrl);

    ProfileResponse response = new ProfileResponse(
        u.getId(),
        u.getUsername(),
        u.getLichessId(),
        u.getTitle(),
        u.getDescription(),
        profileImageUrl,
        bannerImageUrl,
        u.getCreatedAt(),
        u.getLichessCreatedAt(),
        u.getAllGames(),
        u.getRatedGames(),
        u.getWins(),
        u.getLosses(),
        u.getDraws(),
        u.getTotalSeconds()
    );

    log.debug("[ProfileResponse 객체 생성 완료] userId={}", u.getId());

    // Cache-Aside Pattern 3단계: 조회 결과를 캐시에 저장 (TTL: 1시간)
    cacheService.saveCache(cacheKey, response, 3600);
    log.info("[Cache-Set] UserProfile - userId={}, profileImageUrl={}, bannerImageUrl={}, TTL=3600s",
        u.getId(), profileImageUrl, bannerImageUrl);

    log.info("[프로필 조회 완료] userId={}", u.getId());
    return response;
  }

  /**
   * ============================
   * Cache Key Builder Methods
   * ============================
   */
  private String buildProfileCacheKey(Long userId) {
    return String.format("user:profile:%d", userId);
  }

}
