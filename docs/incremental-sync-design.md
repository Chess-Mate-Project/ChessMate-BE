# ChessMate 증분 수집 스케쥴링 설계 문서

> 작성일: 2026-04-13  
> 기준 API: Lichess API v1 (2025-04), Chess.com Public API (2025-04)

---

## 목차

1. [전체 아키텍처](#1-전체-아키텍처)
2. [처리 파이프라인 흐름](#2-처리-파이프라인-흐름)
3. [파일별 변경 상세](#3-파일별-변경-상세)
4. [Lichess 게임 수집 처리](#4-lichess-게임-수집-처리)
5. [Chess.com 게임 수집 처리](#5-chesscom-게임-수집-처리)
6. [집계(Aggregation) 처리](#6-집계aggregation-처리)
7. [중복 집계 방지 3중 방어](#7-중복-집계-방지-3중-방어)
8. [동시 접속 내성 분석](#8-동시-접속-내성-분석)
9. [트레이드오프 정리](#9-트레이드오프-정리)

---

## 1. 전체 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                   4시간 스케쥴러                          │
│              ScheduledSyncTrigger                        │
│       @Scheduled(cron = "0 0 */4 * * *")                │
└────────────────────┬────────────────────────────────────┘
                     │ SyncJob.create() / createWithCursor()
                     │ syncJobRepository.save()
                     │ syncJobProducer.enqueue()
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   Redis 큐 (플랫폼별)                     │
│   queue:sync:lichess  │  queue:sync:chesscom             │
└─────────┬─────────────────────────┬─────────────────────┘
          │ poll (1초마다)           │ poll (1초마다)
          ▼                         ▼
┌──────────────────┐     ┌──────────────────────────────┐
│ SyncJobDispatcher│     │      SyncJobDispatcher        │
│  pollLichess()   │     │      pollChessCom()           │
└────────┬─────────┘     └──────────────┬────────────────┘
         │                              │
         ▼                              ▼
┌──────────────────────┐   ┌──────────────────────────────┐
│ LichessGameSyncWorker│   │   ChessComGameSyncWorker      │
│                      │   │                               │
│  DB에 게임 있음?      │   │  cursor = 전월(yyyy/MM)?      │
│  ├─ YES → incremental│   │  ├─ YES → 당월 archive 1건    │
│  │   since=max+1ms   │   │  │        만 재수집            │
│  └─ NO  → full sync  │   │  └─ NO  → 전체 archive 수집  │
│      until=gameId    │   │                               │
└──────────┬───────────┘   └───────────────┬──────────────┘
           │                               │
           └──────────────┬────────────────┘
                          │ saveAll() (중복 차단)
                          ▼
                    ┌──────────┐
                    │  game DB │
                    └──────────┘
                          │ 신규 저장 > 0 일 때만
                          ▼
                 ┌─────────────────┐
                 │ GameStatAggrega-│
                 │ tor.aggregate() │
                 │ (delete+saveAll)│
                 └─────────────────┘
```

### 설계 원칙

| 원칙 | 적용 |
|------|------|
| 파이프라인 재사용 | Redis 큐 → Dispatcher → Worker 기존 흐름 그대로 사용 |
| API 레벨 필터링 우선 | 불필요한 API 호출 자체를 줄임 |
| DB 레벨 이중 방어 | API 필터링 실패 시에도 saveAll 중복 차단 |
| 조건부 집계 | 신규 게임 없으면 stat 테이블 건드리지 않음 |

---

## 2. 처리 파이프라인 흐름

### 2-1. 최초 로그인 (전체 수집)

```
사용자 OAuth 완료
    │
    ├── LichessOAuthService / ChesscomOAuthService
    │       SyncJob.create() → save → enqueue
    │
    ▼ Redis 큐
    │
    ├── [Lichess] DB에 게임 없음
    │       → processFullSync()
    │         GET /api/games/user/{u}?max=100&until={gameId}&sort=dateDesc&rated=true
    │         100개 청크씩 역방향(최신→과거) 페이징
    │         untilGameId = 마지막 게임 id → 다음 청크 시작점
    │
    └── [Chess.com] cursor=null
            → filterPending(archives, null) = 전체 아카이브
              GET /pub/player/{u}/games/{yyyy}/{MM} × N개월
              병렬(토큰 있을 때) 또는 순차(토큰 없을 때) 처리
```

### 2-2. 정기 스케쥴 (증분 수집, 4시간 간격)

```
ScheduledSyncTrigger (0시/4시/8시/12시/16시/20시)
    │
    ├── lichessUserRepository.findAll() → 전체 유저 루프
    │       SyncJob.create() (cursor 없음)
    │       → enqueue → Worker가 DB 보고 mode 결정
    │
    └── chesscomUserRepository.findAll() → 전체 유저 루프
            lastJob.status == COMPLETED?
            ├── YES: SyncJob.createWithCursor(prevMonth)
            └── NO:  SyncJob.create() (전체 수집 재시도)
            → enqueue
```

---

## 3. 파일별 변경 상세

### 3-1. `chessmate-domain`

#### `game/GameRepository.java` — 메서드 추가

```java
Optional<LocalDateTime> findLatestPlayedAtByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
```

**추가 이유**  
Lichess 증분 수집의 `since` 커서 계산 기준점. DB에 저장된 최신 게임의 `played_at`을 조회해 `+1ms`를 since로 사용.  
`MAX(played_at)`을 JPQL로 바로 뽑기 때문에 전체 게임 로드 없이 O(1) 쿼리.

**트레이드오프**  
- `MAX(played_at)` 쿼리는 인덱스 없으면 풀스캔. `idx_game_user_platform`(user_id, platform) 인덱스 존재하므로 빠름.
- `played_at`이 NULL인 게임이 있으면 MAX 결과가 왜곡될 수 있음. NULL 게임은 Worker 필터에서 이미 제거되므로 문제없음.

---

#### `lichess/user/LichessUserRepository.java` — `findAll()` 추가

```java
List<LichessUser> findAll();
```

**추가 이유**  
스케쥴러가 전체 Lichess 사용자를 순회하기 위해 필요.  
JpaRepository 기본 제공 메서드를 도메인 인터페이스에 노출.

**트레이드오프**  
- 사용자가 수천 명 이상이면 전체 로드가 부담. 현재 규모에서는 문제없음.
- 커진다면 페이지네이션(`findAll(Pageable)`) 또는 ID만 조회하는 프로젝션 쿼리로 교체 권장.

---

#### `chesscom/user/ChesscomUserRepository.java` — `findAll()` 추가

LichessUserRepository와 동일한 이유. Chess.com 사용자 전체 순회용.

---

#### `sync/SyncJob.java` — `createWithCursor()` 팩토리 추가

```java
public static SyncJob createWithCursor(Long userId, OAuthPlatForm platform,
                                        String platformUsername, String cursor) {
    SyncJob job = create(userId, platform, platformUsername);
    job.syncCursor = cursor;
    return job;
}
```

**추가 이유**  
Chess.com 증분 수집에서 스케쥴러가 SyncJob 생성 시점에 cursor를 "전월(yyyy/MM)"로 사전 설정해야 함.  
Worker는 이 cursor를 그대로 `filterPending()`에 넘겨 당월 archive만 처리.

**트레이드오프**  
- cursor 필드를 Lichess(gameId 기반)와 Chess.com(yyyy/MM 기반)이 공유함. 의미가 다름.
- 별도 `IncrementalMode` 필드를 두는 게 더 명시적이지만, 현재 두 Worker가 각자 cursor 의미를 알고 있으므로 오버엔지니어링.

---

### 3-2. `chessmate-infra-persistence`

#### `game/jpaRepository/GameJpaRepository.java` — MAX 쿼리 추가

```java
@Query("SELECT MAX(g.playedAt) FROM GameJpaEntity g WHERE g.userId = :userId AND g.platform = :platform")
Optional<LocalDateTime> findLatestPlayedAtByUserIdAndPlatform(
    @Param("userId") Long userId,
    @Param("platform") OAuthPlatForm platform
);
```

**선택 이유**  
`findTopByUserIdAndPlatformOrderByPlayedAtDesc()` 방식은 엔티티 전체를 로드.  
`MAX()` 집계 함수가 timestamp 하나만 반환해 훨씬 경량.

---

#### `game/repositoryImpl/GameRepositoryImpl.java` — 구현체 위임 추가

```java
@Override
public Optional<LocalDateTime> findLatestPlayedAtByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
    return jpaRepository.findLatestPlayedAtByUserIdAndPlatform(userId, platform);
}
```

---

#### `lichess/user/repositoryImpl/LichessUserRepositoryImpl.java` — `findAll()` 구현

```java
@Override
public List<LichessUser> findAll() {
    return jpaRepository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
}
```

---

#### `chesscom/user/repositoryImpl/ChesscomUserRepositoryImpl.java` — `findAll()` 구현

LichessUserRepositoryImpl과 동일 패턴.

---

### 3-3. `chessmate-external`

#### `api/lichess/LichessApi.java` — `since` 파라미터 추가

```java
// 변경 전
@GetExchange("/api/games/user/{username}")
String getGames(..., @RequestParam(value = "until", required = false) String until, ...);

// 변경 후
@GetExchange("/api/games/user/{username}")
String getGames(
    ...,
    @RequestParam(value = "until", required = false) String until,   // 전체수집: gameId
    @RequestParam(value = "since", required = false) Long since,     // 증분수집: epochMillis
    ...
);
```

**선택 이유**  
Lichess 공식 API(2025-04)는 `since`(epoch ms)를 지원.  
`since`를 사용하면 API 서버가 필터링을 처리하므로 불필요한 게임이 응답에 포함되지 않음.  
클라이언트 필터링보다 대역폭 효율이 좋음.

**트레이드오프**  
- `until`은 gameId(String), `since`는 epochMillis(Long)으로 타입이 다름. 같은 메서드에 두 커서가 공존.  
- 실제 사용 시 둘 중 하나만 전달 (전체수집: until=gameId, since=null / 증분수집: until=null, since=epochMs).  
- 더 깔끔하게 하려면 메서드를 두 개로 분리할 수 있으나 HTTP Interface 특성상 추가 Bean 등록이 필요해 오버헤드.

---

### 3-4. `chessmate-worker`

#### `LichessGameSyncWorker.java` — 전체/증분 자동 분기

| 변경 전 | 변경 후 |
|---------|---------|
| `process()` 단일 메서드, 항상 until 커서 역방향 페이징 | `process()` → DB 상태 확인 후 `processFullSync()` or `processIncremental()` 분기 |
| stat 재집계 항상 실행 | 신규 저장 건수 > 0 일 때만 stat 재집계 |

자세한 내용은 [4. Lichess 게임 수집 처리](#4-lichess-게임-수집-처리) 참고.

---

#### `ChessComGameSyncWorker.java` — 조건부 stat 재집계

| 변경 전 | 변경 후 |
|---------|---------|
| `statAggregator.aggregate()` 항상 실행 | `newlySaved > 0` 조건 추가 |
| cursor 로그 없음 | 수집 시작 로그에 cursor 포함 |

자세한 내용은 [5. Chess.com 게임 수집 처리](#5-chesscom-게임-수집-처리) 참고.

---

#### `ScheduledSyncTrigger.java` — 신규 파일

4시간 간격 전체 사용자 증분 수집 트리거. 자세한 내용은 [2. 처리 파이프라인 흐름](#2-처리-파이프라인-흐름) 참고.

---

## 4. Lichess 게임 수집 처리

### 4-1. API 스펙 (2025-04 기준)

```
GET https://lichess.org/api/games/user/{username}
Authorization: Bearer {access_token}
Accept: application/x-ndjson

파라미터:
  max    : int     최대 게임 수 (최대 200, 우리는 100 고정)
  since  : long    epoch millis, 이 시각 이후 게임만 반환
  until  : string  게임 ID, 이 게임 이전 게임만 반환 (역방향 페이징)
  sort   : string  "dateDesc" (최신→과거) | "dateAsc" (과거→최신)
  rated  : boolean true면 레이팅 게임만

응답: NDJSON (줄바꿈으로 구분된 JSON 스트림)
{"id":"abcd1234","rated":true,"perf":"blitz","createdAt":1710000000000,...}
{"id":"efgh5678",...}
...
```

### 4-2. 전체 수집 흐름 (최초 로그인)

```
[DB에 게임 없음 → processFullSync()]

 1회차: GET ?max=100&until=null&sort=dateDesc&rated=true
        → 최신 100개 게임 수신
        → saveAll() [중복 체크 후 저장]
        → cursor = 마지막 게임 id ("xyz...")

 2회차: GET ?max=100&until=xyz...&sort=dateDesc&rated=true
        → 그 이전 100개 게임 수신
        → saveAll()
        → cursor = 다음 마지막 게임 id

 ...반복 (응답 < 100개면 마지막 페이지)

 완료: statAggregator.aggregate() + perfStatFetcher.fetch()
```

**왜 dateDesc(최신→과거) 방향인가?**  
- 사용자가 중간에 토큰 만료 등으로 수집 중단 시, 최신 게임이 우선 저장됨
- 재시작 시 저장된 마지막 gameId부터 이어서 수집 가능 (cursor 복구)
- 사용자는 최신 통계부터 볼 수 있어 UX 우선순위에 맞음

### 4-3. 증분 수집 흐름 (정기 스케쥴러)

```
[DB에 게임 있음 → processIncremental()]

DB: MAX(played_at) = 2026-04-10 14:23:00 UTC
since = 1712759380001 (epoch ms + 1ms)

 1회차: GET ?max=100&since=1712759380001&sort=dateAsc&rated=true
        → 그 이후 최대 100개 신규 게임 수신
        → 없으면 즉시 종료 (API 레벨 필터링)
        → saveAll() [혹시 모를 중복 차단]
        → since = 마지막 게임.createdAt + 1ms

 2회차: since 갱신 후 반복
        (100개 미만이면 마지막 페이지 → 종료)

 완료 (신규 > 0): statAggregator + perfStatFetcher
 완료 (신규 = 0): 아무것도 하지 않음
```

**왜 dateAsc(과거→최신) 방향인가?**  
- `since` 파라미터와 함께 쓸 때 자연스러운 시간순
- 다음 since 계산이 단순: `마지막 게임.createdAt + 1ms`
- dateDesc를 쓰면 since 이후 게임 중 최신부터 오므로 페이징 시 since 갱신이 직관적이지 않음

### 4-4. Rate Limiting

```java
private static final RateLimiter RATE_LIMITER = RateLimiter.create(0.05);
// 0.05 permits/sec = 20초당 1회 = 분당 3회
// 청크 100개 × 분당 3회 = 분당 300게임 수집
```

**Lichess 공식 Rate Limit (2025-04)**  
- 인증 요청: 20 req/sec (초당 20회)  
- 게임 export 스트리밍: 별도 버킷, 실질적으로 더 관대  
- 우리 설정 0.05 req/s는 Lichess 한도의 1/400 수준 → **극히 보수적**

**왜 이렇게 낮게 설정했나?**  
- 다수 사용자가 동시에 sync 중일 때 Worker 인스턴스 내 공유 RateLimiter가 전체 제한  
- 여러 Worker 스레드가 동시에 돌아도 하나의 static RateLimiter가 제어  
- 실제 트래픽 측정 후 상향 가능 (예: 0.5 req/s = 분당 30회)

### 4-5. 데이터 파싱

```
NDJSON 응답:
{"id":"abc","rated":true,"perf":"blitz","createdAt":1710000000000,
 "players":{"white":{"user":{"name":"magnus"}},"black":{"user":{"name":"opponent"}}},
 "winner":"white","moves":"e4 e5 Nf3","variant":"standard"}

파싱 과정:
1. "\n" 기준 split → 각 줄이 게임 1개
2. ObjectMapper.readValue() → LichessGamesDto record
3. 파싱 실패 줄은 warn 로그 후 스킵 (전체 수집 중단 방지)

필터 조건:
- perf ∈ {blitz, bullet, rapid, classical}  (chess960, antichess 등 제외)
- createdAt != null && createdAt > 0

결과 매핑:
- 플레이어 색상: players.white.user.name == username → WHITE, 아니면 BLACK
- 결과: winner 필드 ("white"/"black"/null) × 내 색상 → WIN/LOSS/DRAW
- played_at: createdAt(epochMillis) → LocalDateTime(UTC)
```

---

## 5. Chess.com 게임 수집 처리

### 5-1. API 스펙 (2025-04 기준)

```
아카이브 목록 조회:
GET https://api.chess.com/pub/player/{username}/games/archives

응답:
{
  "archives": [
    "https://api.chess.com/pub/player/magnus/games/2010/01",
    "https://api.chess.com/pub/player/magnus/games/2010/02",
    ...
    "https://api.chess.com/pub/player/magnus/games/2026/04"
  ]
}

월별 게임 조회:
GET https://api.chess.com/pub/player/{username}/games/{year}/{month}
Authorization: Bearer {access_token}  (선택적 - 공개 데이터는 불필요)

응답:
{
  "games": [
    {
      "url": "https://www.chess.com/game/live/12345",
      "pgn": "[Event ...]\n\n1. e4 e5 ...",
      "time_class": "blitz",
      "time_control": "300+0",
      "rated": true,
      "end_time": 1712000000,
      "white": { "username": "magnus", "result": "win" },
      "black": { "username": "opponent", "result": "checkmated" }
    }
  ]
}

Rate Limit: 공식 명시 없음. 실제 운용 기준 1~2 req/s 권장.
429 응답 시 해당 archive 스킵 (job 전체 실패 방지).
```

### 5-2. 전체 수집 흐름 (최초 로그인)

```
[cursor=null → filterPending(archives, null) = 전체]

Step 1: GET /pub/player/{u}/games/archives
        → ["2020/01", "2020/02", ..., "2026/04"] 전체 목록

Step 2: 토큰 있음 → processParallel() (parallelism=5 기본)
        토큰 없음 → processSequential()

Step 3: 각 archive URL마다 fetchAndSave():
        GET /pub/player/{u}/games/{yyyy}/{MM}
        → 해당 월 전체 게임 응답
        → filter: rated=true AND timeClass ∈ {blitz, bullet, rapid, classical}
        → saveAll()
        → progressAtomic(cursor="yyyy/MM", count)

Step 4: 완료 → statAggregator + perfStatFetcher
```

**병렬 처리 상세:**

```java
ExecutorService executor = Executors.newFixedThreadPool(parallelism);  // 기본 5

archives.stream()
    .map(url -> CompletableFuture.runAsync(() -> {
        PARALLEL_RATE_LIMITER.acquire();  // 2.0 req/s 제한
        fetchAndSave(...);
    }, executor))
    .toList();

CompletableFuture.allOf(futures.toArray(...)).join();  // 전체 완료 대기
executor.shutdown();
```

- 개별 archive 실패 → 해당 archive만 스킵, failCount++, job 전체는 계속 진행  
- 429(Rate Limit) → 스킵  
- 401(Unauthorized) → 토큰 갱신 후 1회 재시도  
- 5xx → 스킵

### 5-3. 증분 수집 흐름 (정기 스케쥴러)

```
[cursor = "2026/03" (전월) → filterPending이 당월만 반환]

Step 1: GET /pub/player/{u}/games/archives
        → ["2020/01", ..., "2026/03", "2026/04"]

Step 2: filterPending(archives, "2026/03")
        "2026/03" 이후 = ["2026/04"] 만 남음

Step 3: fetchAndSave("https://api.chess.com/.../games/2026/04")
        → 2026년 4월 전체 게임 (이미 저장된 것 포함)
        → saveAll() 내부에서 platformGameId 중복 체크
        → 신규 게임만 INSERT

Step 4: 신규 저장 > 0 → statAggregator + perfStatFetcher
        신규 저장 = 0 → 아무것도 하지 않음
```

**왜 "전월"을 cursor로 설정하나?**  
```
Chess.com 아카이브 특성:
- 2026/01, 2026/02, 2026/03 → 해당 월이 끝나면 확정(immutable)
- 2026/04 → 현재 진행 중인 달, 매 시간 새 게임 추가됨

따라서:
- 전월(2026/03)을 cursor로 설정
- filterPending("2026/03") → "2026/03" 이후 = ["2026/04"] 반환
- 당월 archive 1건만 재호출 → API 비용 최소화
```

### 5-4. cursor 동시성 처리

Chess.com은 archive를 병렬 처리하므로 cursor 업데이트에 경쟁이 발생함:

```java
// SyncJob.progressAtomic() - synchronized 처리
public synchronized void progressAtomic(String cursor, int count) {
    this.syncCursor = cursor;           // 마지막 완료 month로 덮어쓰기
    this.atomicFetched.addAndGet(count); // AtomicInteger로 카운트
    this.updatedAt = LocalDateTime.now();
}
```

**트레이드오프**  
- `cursor`(syncCursor 필드)는 병렬에서 덮어쓰기 경쟁 발생 가능  
- 하지만 cursor의 목적은 "중단 시 재시작 지점"이므로, 어느 month가 마지막으로 기록되어도 남은 months는 재처리 가능 → **데이터 정합성에는 영향 없음**  
- count(atomicFetched)는 AtomicInteger로 thread-safe

### 5-5. 데이터 파싱

```
PGN 응답 → platformGameId로 게임 URL 사용:
- platformGameId = dto.getUrl() ("https://www.chess.com/game/live/12345")
- moves = PGN 전체 문자열 (헤더 포함)

결과 매핑:
- white.result / black.result → "win"/"checkmated"/"resigned"/"agreed"... 등 12가지
- WIN: "win"
- DRAW: {"agreed","repetition","stalemate","insufficient","50move","timevsinsufficient","kingofthehill"}
- LOSS: 나머지 ("checkmated","resigned","timeout","abandoned"...)
- OTHER: null 또는 알 수 없는 값

Lichess와 차이:
- Lichess: winner 필드 ("white"/"black"/null) 기준
- Chess.com: 플레이어별 result 필드 기준 → 더 세분화된 종료 이유 포함
- played_at: endTime(epoch seconds) → LocalDateTime(UTC)  [Lichess는 epochMillis]
```

---

## 6. 집계(Aggregation) 처리

### 6-1. 집계 항목

| 테이블 | 집계 내용 | 파티션 키 |
|--------|-----------|-----------|
| `user_daily_game_stat` | 날짜별 총/승/무/패 수 | userId + platform + date |
| `user_color_stat` | (timeClass × 색상)별 승/무/패 | userId + platform + timeClass + color |
| `user_first_move_stat` | (timeClass × 색상 × 첫수)별 빈도 | userId + platform + timeClass + color + move |
| `user_perf_stat` | 타임클래스별 레이팅 곡선 | userId + platform (PerfStatFetcher 별도 처리) |

### 6-2. 집계 전략: Full Recompute (delete → saveAll)

```java
// GameStatAggregator.computeDailyStats() 예시
dailyStatRepository.deleteByUserIdAndPlatform(userId, platform);  // 1. 기존 삭제
dailyStatRepository.saveAll(stats);                               // 2. 재계산 후 전체 저장
```

**왜 증분 집계 대신 Full Recompute인가?**

| 방식 | 장점 | 단점 |
|------|------|------|
| **Full Recompute** (현재) | 구현 단순, 항상 정합성 보장 | 게임 수 비례 메모리/CPU 사용 |
| 증분 집계 (delta only) | 신규 게임만 처리, 빠름 | 동시성 race condition, 구현 복잡 |

- 체스 게임 수는 사용자당 수천~수만 건 수준 → Full Recompute 비용 허용 범위
- 증분 집계는 동시에 두 SyncJob이 실행될 경우 카운트 중복 위험
- **단, `totalNewlySaved > 0` 조건으로 신규 게임 없을 때는 집계 자체를 스킵**하므로 4시간마다 불필요한 recompute 없음

### 6-3. 집계 실행 시점

```
신규 저장 게임 있음 (totalNewlySaved > 0):
    GameStatAggregator.aggregate()
        └─ DB에서 userId+platform 전체 게임 로드
        └─ delete + 재집계 saveAll (3개 stat 테이블)
    PerfStatFetcher.fetch()
        └─ Lichess API /api/user/{u}/perf/{type} 호출
        └─ Chess.com API /pub/player/{u}/stats 호출
        └─ user_perf_stat 업데이트

신규 저장 게임 없음 (totalNewlySaved = 0):
    아무것도 하지 않음 → stat 테이블 변경 없음
```

---

## 7. 중복 집계 방지 3중 방어

### 방어 1: API 레벨 필터링

```
Lichess:
  since = MAX(played_at) + 1ms
  → Lichess 서버가 이 시각 이후 게임만 응답
  → 응답 자체에 이미 저장된 게임 포함 안 됨

Chess.com:
  cursor = 전월(yyyy/MM)
  → filterPending이 당월 archive URL 1건만 반환
  → 과거 달의 archive API 호출 자체가 발생하지 않음
```

### 방어 2: saveAll 중복 체크 (DB 조회 기반)

```java
// GameRepositoryImpl.saveAll()
var requestedIds = games.stream().map(Game::getPlatformGameId).toList();
Set<String> existing = Set.copyOf(
    jpaRepository.findExistingPlatformGameIds(platform, requestedIds)
);

var newGames = games.stream()
    .filter(g -> !existing.contains(g.getPlatformGameId()))
    .map(mapper::toEntity)
    .toList();
```

- 저장하려는 게임 ID 목록을 한 번의 IN 쿼리로 기존 여부 확인
- 이미 있는 게임은 INSERT하지 않음

### 방어 3: DB UNIQUE 제약

```java
// GameJpaEntity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_game_platform_id",
        columnNames = {"platform", "platform_game_id"})
})
```

- 방어 2를 뚫고 INSERT가 시도되면 DB 레벨에서 최종 차단
- 동시 요청에서 race condition 발생 시에도 안전

### 방어 4: 조건부 stat 재집계

```
totalNewlySaved = 0 → statAggregator.aggregate() 호출 안 함
→ stat 테이블에 변경 없음 = "집계"가 발생하지 않음
```

---

## 8. 동시 접속 내성 분석

### 8-1. 사용자 동시 로그인 (User-triggered sync)

```
시나리오: 100명이 동시에 OAuth 완료 → 100개 SyncJob 동시 enqueue

구조:
  Redis 큐: FIFO → 순서 보장
  SyncJobDispatcher: 1초마다 큐에서 1개씩 꺼냄
  Worker: 동기 처리 (한 번에 1개 job 처리)

결과:
  - 큐에 100개 쌓임
  - 순차 처리 → 동시성 문제 없음
  - 앞 사람이 처리되는 동안 뒷사람은 PENDING 상태로 대기
  - RateLimiter가 각 Worker 내에서 독립적으로 제한
```

### 8-2. 스케쥴러 + 사용자 트리거 동시 발생

```
시나리오: 스케쥴러 동작 중 사용자가 수동 sync 트리거

Worker:   [스케쥴 job for userA] ---처리중--- [완료]
Queue:    [..., userA_manual_job, ...]

결과:
  - userA에 대해 두 개의 SyncJob 생성됨
  - 스케쥴 job 완료 후 manual job 처리
  - Lichess: manual job 실행 시 latestPlayedAt이 이미 최신
    → since = 방금 완료된 시점 → 신규 게임 없음 → 즉시 종료
  - Chess.com: manual job cursor=null (사용자 트리거는 cursor 없음)
    → filterPending(null) = 전체 → saveAll 중복 체크로 신규만 저장
```

### 8-3. Lichess static RateLimiter 공유

```java
// Worker 인스턴스는 Spring Singleton
private static final RateLimiter RATE_LIMITER = RateLimiter.create(0.05);
// static → 모든 SyncJob 처리가 이 하나의 RateLimiter 공유
```

```
SyncJob A (userA) → RATE_LIMITER.acquire() → 기다림
SyncJob B (userB) → RATE_LIMITER.acquire() → A 완료 후 실행

결과:
  - 다수 사용자가 동시에 sync해도 Lichess API로의 요청은 0.05/s 유지
  - Worker 스레드는 RateLimiter에서 블로킹되므로 CPU는 거의 사용 안 함
  - 실질적으로 직렬화됨 → Lichess API ban 위험 없음
```

### 8-4. Chess.com 병렬 처리와 thread-safety

```java
// progressAtomic: synchronized 메서드
public synchronized void progressAtomic(String cursor, int count) {
    this.syncCursor = cursor;
    this.atomicFetched.addAndGet(count);
}
```

```
5개 스레드(parallelism=5)가 동시에 archive 처리:

Thread-1: 2026/01 완료 → progressAtomic("2026/01", 50)
Thread-2: 2026/02 완료 → progressAtomic("2026/02", 30)
Thread-3: 2026/03 완료 → progressAtomic("2026/03", 45)
...동시 실행...

atomicFetched: AtomicInteger → 덧셈 연산 thread-safe ✓
syncCursor:    synchronized → 한 번에 하나만 쓰기 ✓
              (어느 month가 마지막 cursor가 되어도 재처리 가능)
```

### 8-5. 동시 접속 한계점

| 시나리오 | 현재 대응 | 한계 |
|----------|-----------|------|
| 100명 동시 로그인 | Redis 큐 버퍼 | 각자 대기 시간 증가 |
| 1000명 스케쥴 동시 enqueue | Redis 큐 순차 처리 | 큐 드레인까지 시간 |
| Lichess 0.05 req/s 전역 제한 | 순차 처리로 안전 | 처리량 낮음 |
| saveAll race condition | DB UNIQUE 제약 | 중복 시도는 INSERT 실패 (정합성 유지) |

---

## 9. 트레이드오프 정리

### 9-1. 아키텍처 선택

| 결정 | 선택 | 대안 | 선택 이유 |
|------|------|------|-----------|
| 스케쥴러 구현 | Redis 큐 재사용 | 직접 Worker 호출 | 기존 rate limiter/에러처리 파이프라인 재사용, 장애 복구 가능 |
| Lichess 증분 커서 | `since` timestamp | gameId 기반 until | API 공식 지원, 의미가 명확, 응답에 불필요한 게임 없음 |
| Chess.com 증분 전략 | cursor=전월 + DB dedup | since 필터 없음 (API 미지원) | API가 since 미지원 → 월별 archive 특성 이용 |
| stat 재집계 방식 | Full Recompute 유지 | 증분 집계 | 동시성 race condition 위험 제거, 구현 단순, 규모상 허용 |
| 모드 분기 위치 | Lichess Worker 내부 | Scheduler 외부 지정 | Worker가 DB 상태를 직접 알 수 있음, 스케쥴러-워커 결합 없음 |
| Chess.com 모드 분기 위치 | Scheduler cursor 사전 설정 | Worker 내부 판단 | Archive API 특성상 cursor로 제어가 더 자연스러움 |

### 9-2. 알려진 제약

**Chess.com 당월 archive 재다운로드**  
증분 수집마다 당월 전체 게임 목록을 API에서 받아옴. 당월에 게임이 많은 사용자(수백 게임/월)는 4시간마다 전체 목록을 받고 saveAll에서 필터링.  
→ 개선 방안: Chess.com이 `end_time` 필터를 지원하지 않으므로 현재로선 최선. 향후 DB에서 당월 저장된 게임 ID Set을 캐싱하면 API 응답 처리 속도 향상 가능.

**Lichess `played_at` NULL 게임**  
`createdAt=0` 또는 null인 게임은 `MAX(played_at)` 계산에서 제외됨. 이런 게임이 since 기준점보다 오래된 시각으로 기록되면 누락될 수 있음.  
→ 현재: 필터 조건 `g.createdAt() != null && g.createdAt() > 0`으로 이런 게임은 저장 자체를 안 함. 실질적 영향 없음.

**스케쥴러 중복 실행**  
서버가 여러 인스턴스로 실행 중이면 각 인스턴스가 독립적으로 스케쥴러를 실행해 중복 enqueue 발생 가능.  
→ 현재: 단일 인스턴스 구성 가정. 다중 인스턴스 환경에서는 `ShedLock` 또는 Redis 분산락 필요.

**사용자 증가 시 findAll() 성능**  
`lichessUserRepository.findAll()`이 전체 사용자를 한 번에 로드.  
→ 수천 명 이상이면 페이지네이션(`Pageable`) 또는 ID만 조회하는 프로젝션으로 교체 권장.