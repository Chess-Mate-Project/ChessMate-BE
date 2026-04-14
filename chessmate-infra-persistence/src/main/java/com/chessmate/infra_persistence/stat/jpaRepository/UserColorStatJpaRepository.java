package com.chessmate.infra_persistence.stat.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.stat.entity.UserColorStatJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserColorStatJpaRepository extends JpaRepository<UserColorStatJpaEntity, Long> {
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserColorStatJpaEntity> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    List<UserColorStatJpaEntity> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass);
}