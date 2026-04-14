package com.chessmate.domain.game;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.game.MonthlyRating;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameRepository {
    List<Game> saveAll(List<Game> games);
    List<Game> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    /**
     * 증분 수집용: 해당 유저의 플랫폼 최근 게임 played_at 조회.
     * 값이 있으면 since 커서로 사용해 신규 게임만 API 요청합니다.
     */
    Optional<LocalDateTime> findLatestPlayedAtByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    /**
     * 최근 1년간 월별 마지막 레이팅 조회.
     * timeClass가 null이면 전체 타임클래스 반환.
     */
    List<MonthlyRating> findMonthlyLastRating(Long userId, OAuthPlatForm platform, String timeClass, LocalDateTime since);
}