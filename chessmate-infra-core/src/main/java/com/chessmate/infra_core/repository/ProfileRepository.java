package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.Profile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
  Optional<Profile> findById(Long id);

  /**
   * 사용자의 프로필 이미지를 업데이트합니다.
   *
   * @param id 사용자 ID
   * @param key S3 저장 경로 (예: users/140/profile.jpg)
   */
  @Modifying
  @Query("UPDATE Profile p SET p.profile = :key WHERE p.id = :id")
  void updateProfile(@Param("id") Long id, @Param("key") String key);

  /**
   * 사용자의 배너 이미지를 업데이트합니다.
   *
   * @param id 사용자 ID
   * @param key S3 저장 경로 (예: users/140/banner.jpg)
   */
  @Modifying
  @Query("UPDATE Profile p SET p.banner = :key WHERE p.id = :id")
  void updateBanner(@Param("id") Long id, @Param("key") String key);
}
