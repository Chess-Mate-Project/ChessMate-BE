# LichessGameSyncWorker 정리

이 문서는 `LichessGameSyncWorker`의 동작 흐름을 한눈에 이해하기 쉽도록 정리한 문서입니다. 코드 전체를 포함하며, 파라미터, 처리 로직, 예외처리/개선 포인트를 함께 제공합니다.

---

## 요약

- 목적: Lichess 계정의 게임을 청크 단위로 받아와서 DB에 저장하고 통계/성능 정보 수집을 트리거한다.
- 핵심 제어 흐름: 토큰 확인 → job 시작 → 반복(fetch 100개씩 NDJSON) → 필터/변환 → 저장 → 커서 저장 → 완료/예외처리
- 중요 설정
  - CHUNK_SIZE = 100
  - RateLimiter = 0.05 permits/sec (약 분당 3회 호출)
  - NDJSON 수신 (application/x-ndjson)
  - 저장 조건: rated=true && time_class ∈ {blitz, bullet, rapid, classical}

---

## 처리 순서 (높은 수준)

1. `SyncJob`에서 `userId`, `platformUsername` 가져옴
2. `PlatformTokenStore`에서 access token 조회
   - 토큰 없으면 job.expireToken() 후 저장하고 종료
3. job.start(), DB에 저장
4. `untilGameId` = job.getSyncCursor() 로 초기화
5. 무한 루프:
   - RateLimiter.acquire()
   - 토큰 재조회(중간 만료 가능성 대비)
     - 없으면 job.expireToken() 저장 후 종료
   - `lichessApi.getGames(...)` 호출 (NDJSON)
   - NDJSON 파싱 → DTO 리스트
   - 리스트 비었으면 반복 종료
   - 필터 적용 및 DTO → `Game` 엔티티 매핑
   - `gameRepository.saveAll()`로 일괄 저장
   - `untilGameId`를 마지막 게임 id로 갱신
   - job.progress(untilGameId, savedSize) 저장
   - 만약 games.size() < CHUNK_SIZE 이면 종료
6. job.complete() 저장
7. 통계 집계 및 퍼포먼스 수집 트리거

---

## 파라미터 설명 (getGames 호출)

- Authorization: "Bearer " + token
- Accept/Content type: `application/x-ndjson`
- username: 플랫폼 사용자명
- max (CHUNK_SIZE): 한 번에 받아올 게임 수 (100)
- until: `untilGameId` — 마지막으로 처리한 게임의 id를 넘겨 페이징
- 정렬: "dateDesc" (최신부터)
- 다른 플래그: boolean (예: rated only?) — 현재 코드에서 마지막 파라미터 `true`로 전달

> 페이징 방식: Lichess API의 NDJSON 엔드포인트는 `max`와 `until`을 사용해 페이지네이션을 구현합니다. `until`에 마지막으로 처리한 게임 id를 넣으면 해당 id 이전(또는 이후, API 규격에 따름)의 항목을 가져옵니다. (코드에서는 마지막 게임 id를 저장해 다음 호출 시 이어서 가져오도록 함)

---

## 데이터 처리 정책

- 필터링
  - 시간 클래스가 허용된 값이어야 함: blitz, bullet, rapid, classical
  - `createdAt`(epoch millis)가 null이 아니고 > 0
- 매핑(`toGame`)
  - userId, platform(LICHESS), platformGameId(dto.id())
  - username(우리 서비스에서 사용하는 플랫폼 username)
  - opponentUsername: players.white|black에서 반대편 유저 이름 추출
  - playerColor: 현재 유저가 white인지 black인지 판단
  - result: dto.winner()와 playerColor를 비교하여 WIN/LOSS/DRAW 결정
  - playedAt: epoch millis → UTC LocalDateTime 변환
- 저장
  - 일괄 저장(`saveAll`) 후 성공 갯수를 로그에 남김
  - 청크 완료 후 `SyncJob`에 cursor와 진행정보 저장(청크 단위 커서 저장으로 재시작 안전성 확보)

---

## 에러 처리 및 예외 상황

- 토큰 없음: job.expireToken() 후 저장하고 작업 종료
- NDJSON 파싱 실패: 로그로 경고 후 해당 라인 스킵 (파서 내에서 예외 캐치)
- 전체 루프에서 예외 발생 시 catch하여 job.fail(e.getMessage()) 후 저장
- Redis/IO 타임아웃(Worker 로그 참고): Redis 명령 타임아웃 등은 **외부 리소스 문제**이므로 retry/backoff를 권장

---

## 성능 및 운영 고려

- RateLimiter(0.05) 설정
  - 0.05 permits/sec = 1 permit / 20초 = 약 3회/분
  - 한 번에 100개를 받아올 수 있으므로 초당 처리량은 API 규정과 DB 쓰기 성능에 따라 조절
- 청크 크기
  - CHUNK_SIZE를 높이면 호출 횟수는 줄지만 단일 호출 실패 시 롤백 범위 커짐
  - 네트워크/메모리 한계, API rate limit에 따라 적절히 조정
- idempotency
  - 동일한 platformGameId 중복 저장 방지(유니크 제약 또는 업서트 로직 권장)
- streaming 파싱
  - 현재는 NDJSON 전체 문자열을 한 번에 읽어오고 split/parse 함
  - 대용량 처리 안정성을 위해 스트리밍(parsing line-by-line input stream) 방식 권장

---

## 제안되는 개선 사항

1. 토큰 refresh 자동 시도
   - `tokenStore`에 refresh token이 존재하면 access token 만료 시 자동으로 refresh 시도 후 재시도
2. Redis 명령(Queue) 사용 시 타임아웃/재시도 정책
   - RedisCommandTimeoutException 발생 시 exponential backoff로 재시도
3. 저장의 idempotency 강화
   - DB 레벨에서 (platform, platformGameId, userId) 조합 유니크 제약
   - saveAll 전에 이미 존재하는 platformGameId는 스킵하거나 업데이트
4. NDJSON 스트리밍 파서로 변경
   - 메모리 사용량 및 대량 데이터 안정성 개선
5. 모니터링/메트릭
   - 실패 횟수, 청크당 평균 DB 저장 시간, API 응답 시간 등 메트릭 수집

---

## 관련 구성값(권장)

- `worker.sync.chunkSize` (기본 100)
- `worker.sync.rateLimitPermitsPerSec` (기본 0.05)
- `redis.timeoutSeconds` (Queue 관련 타임아웃)
- `game.save.batchSize` (JPA batch/save tuning)

---

## 주요 코드 (전체)

아래는 실제 프로젝트의 `LichessGameSyncWorker.java` 전체 코드입니다. (원본 그대로 첨부)

```java
package com.chessmate.worker;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.game.Game;
import com.chessmate.domain.game.GameRepository;
import com.chessmate.domain.game.GameResult;
import com.chessmate.domain.sync.SyncJob;
import com.chessmate.domain.sync.SyncJobRepository;
import com.chessmate.external.api.lichess.LichessApi;
import com.chessmate.external.dto.game.LichessGamesDto;
import com.chessmate.external.dto.game.Player;
import com.chessmate.infra_redis.token.PlatformTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Lichess 게임 수집 Worker.
 *
 * - 순차 처리 (병렬 불가)
 * - 토큰 필수: 없으면 즉시 TOKEN_EXPIRED
 * - RateLimiter: 0.05 permits/sec = 분당 3회 (100개 × 3 = 300게임/분)
 * - 매 청크 완료 후 cursor DB 저장 → 중간 장애 시 재시작 안전
 * - 레이팅 게임(rated=true) + 지원 타입(blitz/bullet/rapid/classical)만 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LichessGameSyncWorker {

    private static final int CHUNK_SIZE = 100;
    private static final String NDJSON = "application/x-ndjson";
    private static final Set<String> SUPPORTED_TIME_CLASSES = Set.of("blitz", "bullet", "rapid", "classical");

    @SuppressWarnings("UnstableApiUsage")
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(0.05);

    private final LichessApi lichessApi;
    private final PlatformTokenStore tokenStore;
    private final SyncJobRepository syncJobRepository;
    private final GameRepository gameRepository;
    private final GameStatAggregator statAggregator;
    private final PerfStatFetcher perfStatFetcher;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("UnstableApiUsage")
    public void process(SyncJob job) {
        Long userId = job.getUserId();
        String username = job.getPlatformUsername();

        String accessToken = tokenStore.getLichessAccessToken(userId);
        if (accessToken == null) {
            log.warn("[LichessWorker] 토큰 없음 userId={}", userId);
            job.expireToken();
            syncJobRepository.save(job);
            return;
        }

        job.start();
        syncJobRepository.save(job);
        log.info("[LichessWorker] 수집 시작 userId={} username={} jobId={}", userId, username, job.getId());

        try {
            String untilGameId = job.getSyncCursor();

            while (true) {
                RATE_LIMITER.acquire();

                String token = tokenStore.getLichessAccessToken(userId);
                if (token == null) {
                    job.expireToken();
                    syncJobRepository.save(job);
                    return;
                }

                String ndjson = lichessApi.getGames(
                    "Bearer " + token,
                    NDJSON,
                    username,
                    CHUNK_SIZE,
                    untilGameId,
                    "dateDesc",
                    true
                );

                List<LichessGamesDto> games = parseNdjson(ndjson);
                if (games.isEmpty()) break;

                List<Game> toSave = games.stream()
                    .filter(g -> SUPPORTED_TIME_CLASSES.contains(g.perf()))
                    .filter(g -> g.createdAt() != null && g.createdAt() > 0)
                    .map(g -> toGame(userId, username, g))
                    .toList();

                List<Game> saved = gameRepository.saveAll(toSave);
                log.info("[LichessWorker] 청크 {}개 수집 → {}개 저장", games.size(), saved.size());

                untilGameId = games.get(games.size() - 1).id();
                job.progress(untilGameId, saved.size());
                syncJobRepository.save(job);

                if (games.size() < CHUNK_SIZE) break;
            }

            job.complete();
            syncJobRepository.save(job);
            log.info("[LichessWorker] 수집 완료 userId={} total={}", userId, job.getTotalFetched());

            statAggregator.aggregate(userId, OAuthPlatForm.LICHESS);
            perfStatFetcher.fetch(userId, OAuthPlatForm.LICHESS, username);

        } catch (Exception e) {
            log.error("[LichessWorker] 수집 실패 userId={} error={}", userId, e.getMessage(), e);
            job.fail(e.getMessage());
            syncJobRepository.save(job);
        }
    }

    private Game toGame(Long userId, String username, LichessGamesDto dto) {
        boolean isWhite = isWhitePlayer(dto, username);
        String playerColor = isWhite ? "WHITE" : "BLACK";
        String opponentUsername = resolveOpponentUsername(dto, isWhite);
        GameResult result = resolveResult(dto.winner(), playerColor);

        return Game.builder()
            .userId(userId)
            .platform(OAuthPlatForm.LICHESS)
            .platformGameId(dto.id())
            .username(username)
            .opponentUsername(opponentUsername)
            .playerColor(playerColor)
            .result(result)
            .timeClass(dto.perf())
            .rated(dto.rated())
            .moves(dto.moves())
            .variant(dto.variant())
            .playedAt(epochMillisToLocalDateTime(dto.createdAt()))
            .createdAt(LocalDateTime.now())
            .build();
    }

    private boolean isWhitePlayer(LichessGamesDto dto, String username) {
        if (dto.players() == null || dto.players().white() == null) return false;
        Player white = dto.players().white();
        return white.user() != null && username.equalsIgnoreCase(white.user().name());
    }

    private String resolveOpponentUsername(LichessGamesDto dto, boolean isWhite) {
        if (dto.players() == null) return null;
        Player opponent = isWhite ? dto.players().black() : dto.players().white();
        if (opponent == null || opponent.user() == null) return null;
        return opponent.user().name();
    }

    private GameResult resolveResult(String winner, String playerColor) {
        if (winner == null) return GameResult.DRAW;
        boolean playerIsWhite = "WHITE".equals(playerColor);
        boolean whiteWon = "white".equals(winner);
        return (playerIsWhite == whiteWon) ? GameResult.WIN : GameResult.LOSS;
    }

    private LocalDateTime epochMillisToLocalDateTime(Long epochMillis) {
        if (epochMillis == null || epochMillis <= 0) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    private List<LichessGamesDto> parseNdjson(String ndjson) {
        List<LichessGamesDto> result = new ArrayList<>();
        if (ndjson == null || ndjson.isBlank()) return result;

        for (String line : ndjson.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                result.add(objectMapper.readValue(line, LichessGamesDto.class));
            } catch (Exception e) {
                log.warn("[LichessWorker] NDJSON 파싱 실패: {}", line);
            }
        }
        return result;
    }
}
```

---

## 빠른 체크리스트

- [x] 토큰 조회 및 없음 처리
- [x] CHUNK 단위 NDJSON 수신/파싱
- [x] 필터/매핑 후 saveAll
- [x] cursor(=untilGameId) 저장으로 재시작 가능
- [x] 통계/퍼포먼스 집계 후처리

---

필요하시면 다음 작업을 바로 해드리겠습니다:
- `getGames` API 호출에 대한 스트리밍/예외 재시도 로직 추가
- Redis timeout(retry/backoff) 처리 추가
- DB에 platformGameId 유니크 제약 추가 및 중복 처리 로직

원하시면 어떤 것을 우선할지 알려주세요.

