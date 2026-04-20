package com.chessmate.infra_persistence.game.repositoryImpl;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.game.Game;
import com.chessmate.domain.game.GameRepository;
import com.chessmate.domain.game.MonthlyRating;
import com.chessmate.infra_persistence.game.jpaRepository.GameJpaRepository;
import com.chessmate.infra_persistence.game.mapper.GameMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepository {

    private final GameJpaRepository jpaRepository;
    private final GameMapper mapper;

    /**
     * 중복 게임(platform_game_id 기준)을 제외하고 일괄 저장합니다.
     * 이미 저장된 게임은 조용히 스킵됩니다.
     */
    @Override
    public List<Game> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findByUserIdAndPlatform(userId, platform)
            .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Game> saveAll(List<Game> games) {
        if (games.isEmpty()) return List.of();

        // 중복 체크: 같은 플랫폼 내에서 이미 저장된 platformGameId 조회
        var platform = games.get(0).getPlatform();
        var requestedIds = games.stream().map(Game::getPlatformGameId).toList();
        Set<String> existing = Set.copyOf(
            jpaRepository.findExistingPlatformGameIds(platform, requestedIds)
        );

        var newGames = games.stream()
            .filter(g -> !existing.contains(g.getPlatformGameId()))
            .map(mapper::toEntity)
            .toList();

        if (newGames.isEmpty()) {
            log.debug("[GameRepository] 신규 게임 없음 (전체 {}개 중복)", games.size());
            return List.of();
        }

        var saved = jpaRepository.saveAll(newGames);
        log.debug("[GameRepository] {}개 저장 완료 ({}개 중복 스킵)", saved.size(), existing.size());
        return saved.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<LocalDateTime> findLatestPlayedAtByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findLatestPlayedAtByUserIdAndPlatform(userId, platform);
    }

    @Override
    public List<MonthlyRating> findMonthlyLastRating(Long userId, OAuthPlatForm platform, String timeClass, LocalDateTime since) {
        return jpaRepository.findMonthlyLastRating(userId, platform, timeClass, since).stream()
            .map(p -> new MonthlyRating(p.getTimeClass(), p.getYear(), p.getMonth(), p.getRating()))
            .toList();
    }
}