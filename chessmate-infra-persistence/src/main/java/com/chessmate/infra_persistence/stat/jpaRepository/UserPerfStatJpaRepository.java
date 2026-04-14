package com.chessmate.infra_persistence.stat.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.stat.entity.UserPerfStatJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPerfStatJpaRepository extends JpaRepository<UserPerfStatJpaEntity, Long> {

    List<UserPerfStatJpaEntity> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}
