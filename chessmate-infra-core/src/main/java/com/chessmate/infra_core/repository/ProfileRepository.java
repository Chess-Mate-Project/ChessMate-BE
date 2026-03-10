package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.OAuthPlatForm;
import com.chessmate.infra_core.entity.Profile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
  Optional<Profile> findById(Long id);

  /**
   * 플랫폼 ID와 플랫폼으로 프로필을 조회합니다.
   *
   * @param platformId 플랫폼별 고유 ID (Lichess ID 또는 Chess.com Player ID)
   * @param platform OAuth 플랫폼 (LICHESS, CHESSCOM)
   * @return 해당 프로필의 Optional
   */
  Optional<Profile> findByPlatformIdAndPlatform(String platformId, OAuthPlatForm platform);

  /**
   * 사용자의 프로필 이미지를 업데이트합니다.
   *
   * @param id 프로필 ID
   * @param key S3 저장 경로 (예: users/140/profile.jpg)
   */
  @Modifying
  @Query("UPDATE Profile p SET p.profileUrl = :key WHERE p.id = :id")
  void updateProfile(@Param("id") Long id, @Param("key") String key);

  /**
   * 사용자의 배너 이미지를 업데이트합니다.
   *
   * @param id 프로필 ID
   * @param key S3 저장 경로 (예: users/140/banner.jpg)
   */
  @Modifying
  @Query("UPDATE Profile p SET p.bannerUrl = :key WHERE p.id = :id")
  void updateBanner(@Param("id") Long id, @Param("key") String key);
}
