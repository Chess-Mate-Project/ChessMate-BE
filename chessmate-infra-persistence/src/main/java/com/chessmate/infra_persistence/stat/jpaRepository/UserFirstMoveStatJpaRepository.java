package com.chessmate.infra_persistence.stat.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.stat.entity.UserFirstMoveStatJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFirstMoveStatJpaRepository extends JpaRepository<UserFirstMoveStatJpaEntity, Long> {
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserFirstMoveStatJpaEntity> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    List<UserFirstMoveStatJpaEntity> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass);
}