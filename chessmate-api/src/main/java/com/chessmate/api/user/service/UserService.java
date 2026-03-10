//package com.chessmate.api.user.service;
//
//
//import com.chessmate.api.image.ImageUtil;
//import com.chessmate.api.user.dto.ProfileResponse;
//import com.chessmate.api.user.dto.TotalUserCountResponse;
//import com.chessmate.api.user.dto.UpdateUserDescriptionRequest;
//import com.chessmate.common.code.UserErrorCode;
//import com.chessmate.common.exception.UserException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class UserService {
//
//  private final ImageUtil imageUtil;
//
//  public TotalUserCountResponse getTotalUserCount() {
//    long totalUsers = userRepository.count();
//    return new TotalUserCountResponse((int) totalUsers);
//  }
//
//  /**
//   * 사용자 자기소개 업데이트
//   * @param user 현재 사용자
//   * @param updateUserDescriptionRequest 사용자 자기소개 업데이트 요청 DTO
//   */
//  @Transactional
//  public void updateUserDescription(User user,
//      UpdateUserDescriptionRequest updateUserDescriptionRequest) {
//    log.info("[자기소개 업데이트 시작] userId={}, newDescription={}",
//        user.getId(), updateUserDescriptionRequest.description());
//
//    User u = userRepository.findById(user.getId()).orElseThrow(
//        () -> new UserException(UserErrorCode.NOT_FOUND_USER)
//    );
//
//    String oldDescription = u.getDescription();
//    log.debug("[변경 전] userId={}, oldDescription={}", u.getId(), oldDescription);
//
//    u.setDescription(updateUserDescriptionRequest.description());
//    log.debug("[메모리 변경 완료] userId={}, newDescription={}", u.getId(), updateUserDescriptionRequest.description());
//
//    User savedUser = userRepository.save(u);
//    log.info("[DB 저장 완료] userId={}, savedDescription={}, 저장된 객체 id={}",
//        savedUser.getId(), savedUser.getDescription(), savedUser.getId());
//
//    // 캐시 무효화 - 프로필 정보가 변경되었으므로 캐시 삭제
//    String cacheKey = buildProfileCacheKey(u.getId());
//    cacheService.deleteCache(cacheKey);
//    log.info("[Cache-Invalidate] UserProfile - userId={}, reason=description_updated", u.getId());
//  }
//
//  /**
//   * 사용자 프로필 조회 (Cache-Aside Pattern 적용)
//   * - Cache-Hit: 캐시에서 즉시 반환
//   * - Cache-Miss: DB 조회 후 캐시 저장
//   */
//  public ProfileResponse getUserProfile(User user) {
//    log.info("[프로필 조회 시작] userId={}", user.getId());
//
//    // Cache-Aside Pattern 1단계: 캐시에서 조회
//    String cacheKey = buildProfileCacheKey(user.getId());
//    ProfileResponse cachedData = cacheService.getCache(cacheKey, ProfileResponse.class);
//
//    if (cachedData != null) {
//      log.info("[Cache-Hit] UserProfile - userId={}, cachedDescription={}, profileImageUrl={}, bannerImageUrl={}",
//          user.getId(), cachedData.description(), cachedData.profileImage(), cachedData.bannerImage());
//      return cachedData;
//    }
//
//    log.info("[Cache-Miss] UserProfile - userId={}, DB 조회 시작", user.getId());
//
//    // Cache-Aside Pattern 2단계: 캐시 미스 시 DB에서 조회
//    User u = userRepository.findById(user.getId()).orElseThrow(
//        () -> new UserException(UserErrorCode.NOT_FOUND_USER)
//    );
//
//    log.debug("[DB 조회 완료] userId={}, dbDescription={}, profileImage={}, bannerImage={}",
//        u.getId(), u.getDescription(), u.getProfileImage(), u.getBannerImage());
//
//    // 이미지 URL 생성
//    String profileImageUrl = imageUtil.getProfileImageUrl(u);
//    String bannerImageUrl = imageUtil.getBannerImageUrl(u);
//
//    log.info("[이미지 URL 생성 완료] userId={}, profileUrl={}, bannerUrl={}",
//        u.getId(), profileImageUrl, bannerImageUrl);
//
//    ProfileResponse response = new ProfileResponse(
//        u.getId(),
//        u.getUsername(),
//        u.getLichessId(),
//        u.getTitle(),
//        u.getDescription(),
//        profileImageUrl,
//        bannerImageUrl,
//        u.getCreatedAt(),
//        u.getLichessCreatedAt(),
//        u.getAllGames(),
//        u.getRatedGames(),
//        u.getWins(),
//        u.getLosses(),
//        u.getDraws(),
//        u.getTotalSeconds()
//    );
//
//    log.debug("[ProfileResponse 객체 생성 완료] userId={}, description={}", u.getId(), response.description());
//
//    // Cache-Aside Pattern 3단계: 조회 결과를 캐시에 저장 (TTL: 1시간)
//    cacheService.saveCache(cacheKey, response, 3600);
//    log.info("[Cache-Set] UserProfile - userId={}, description={}, profileImageUrl={}, bannerImageUrl={}, TTL=3600s",
//        u.getId(), response.description(), profileImageUrl, bannerImageUrl);
//
//    log.info("[프로필 조회 완료] userId={}", u.getId());
//    return response;
//  }
//
//  /**
//   * 회원 탈퇴 (Hard Delete)
//   * 사용자와 관련된 모든 데이터를 삭제합니다.
//   * @param user 현재 사용자
//   */
//  @Transactional
//  public void withdraw(User user) {
//    Long userId = user.getId();
//    String lichessId = user.getLichessId();
//    log.info("[회원 탈퇴 시작] userId={}, lichessId={}", userId, lichessId);
//
//    // 1. 통계 데이터 삭제 (DB)
//    log.debug("[회원 탈퇴 - 데이터 삭제] UserPerf 삭제");
//    userPerfRepository.deleteAllByUserId(userId);
//
//    log.debug("[회원 탈퇴 - 데이터 삭제] UserDailyStreak 삭제");
//    userDailyStreakRepository.deleteAllByUserId(userId);
//
//    log.debug("[회원 탈퇴 - 데이터 삭제] UserColorStat 삭제");
//    userColorStatRepository.deleteAllByUserId(userId);
//
//    log.debug("[회원 탈퇴 - 데이터 삭제] UserFirstMoveStat 삭제");
//    userFirstMoveStatRepository.deleteAllByUserId(userId);
//
//    // 2. 사용자 정보 삭제 (DB)
//    log.debug("[회원 탈퇴 - 데이터 삭제] User 정보 삭제");
//    userRepository.deleteById(userId);
//
//    // 3. 캐시 및 보안 데이터 삭제 (Redis)
//    log.debug("[회원 탈퇴 - 캐시 삭제] Redis 데이터 청소 시작");
//
//    // 프로필 캐시 삭제
//    cacheService.deleteCache(buildProfileCacheKey(userId));
//
//    // 인증 관련 토큰 삭제
//    cacheService.deleteRefreshToken(userId);
//    cacheService.deleteLichessToken(userId);
//
//    // Lichess API 기반 캐시 삭제
//    if (lichessId != null) {
//      cacheService.deletePlayTime(lichessId);
//      cacheService.deletePerfs(lichessId);
//      cacheService.deleteUserCount(lichessId);
//    }
//
//    // 모든 하위 통계 캐시 삭제 (stat:*:userId:* 패턴)
//    cacheService.deleteAllUserStats(userId);
//
//    log.info("[회원 탈퇴 완료] userId={}", userId);
//  }
//
//  /**
//   * ============================
//   * Cache Key Builder Methods
//   * ============================
//   */
//  private String buildProfileCacheKey(Long userId) {
//    return String.format("user:profile:%d", userId);
//  }
//
//}
