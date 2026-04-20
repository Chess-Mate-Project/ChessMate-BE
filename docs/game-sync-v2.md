# ChessMate 게임 수집 시스템 v2

> 작성일: 2026-04-06  
> 대상 브랜치: HEAD (develop 기준)

---

## 목차

1. [개요](#1-개요)
2. [전체 흐름](#2-전체-흐름)
3. [모듈별 구현 상세](#3-모듈별-구현-상세)
   - 3.1 chessmate-domain
   - 3.2 chessmate-infra-persistence
   - 3.3 chessmate-infra-redis
   - 3.4 chessmate-external
   - 3.5 chessmate-worker
   - 3.6 chessmate-api
4. [데이터 수집 전략](#4-데이터-수집-전략)
5. [game 테이블 DDL](#5-game-테이블-ddl)
6. [미결 사항 및 다음 단계](#6-미결-사항-및-다음-단계)

---

## 1. 개요

### 목적

OAuth 로그인 완료 후 사용자의 Lichess / Chess.com 게임 이력을 비동기 수집하여 DB에 저장한다.  
수집된 raw game 데이터를 기반으로 통계(streak, 색상별 승률, 첫 수 통계)를 집계한다.

### 설계 원칙

| 원칙 | 내용 |
|------|------|
| 비동기 큐 | Redis List 큐를 통해 API 서버와 Worker 서버를 분리 |
| 재시작 안전 | 매 청크/아카이브 완료 시 cursor 저장 → 중간 장애 후 이어서 수집 가능 |
| 중복 방지 | `(platform, platform_game_id)` unique 제약으로 DB 레벨 중복 차단 |
| Rate Limit 준수 | Lichess: 0.05 req/sec, Chess.com 비토큰: 0.5 req/sec |
| 레이팅 게임만 수집 | rated=true + blitz/bullet/rapid/classical 타입만 저장 |

---

## 2. 전체 흐름

```
[사용자 OAuth 로그인]
        │
        ▼
[ChesscomOAuthService / LichessOAuthService]
  SyncJob.create() → syncJobRepository.save()
  syncJobProducer.enqueue(platform, jobId)
        │
        ▼ (Redis List: queue:sync:lichess / queue:sync:chesscom)
        │
        ▼
[SyncJobDispatcher] @Scheduled(fixedDelay=1000ms)
  consumer.poll(platform) → syncJobRepository.findById(jobId)
        │
        ├──(LICHESS)──▶ LichessGameSyncWorker.process(job)
        └──(CHESSCOM)─▶ ChessComGameSyncWorker.process(job)
                │
                ▼
        gameRepository.saveAll(games)  ← rated + 지원 타입 필터 적용
                │
                ▼
        [game 테이블] ← platform_game_id 중복 스킵
```

---

## 3. 모듈별 구현 상세

### 3.1 chessmate-domain

#### `sync/SyncStatus.java`
```
PENDING → IN_PROGRESS → COMPLETED
                      ↘ FAILED
                      ↘ TOKEN_EXPIRED
```

#### `sync/SyncJob.java`

도메인 객체. 수집 작업 하나의 생명주기를 관리.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 서비스 사용자 ID |
| platform | OAuthPlatForm | LICHESS / CHESSCOM |
| platformUsername | String | 플랫폼 계정명 |
| status | SyncStatus | 현재 상태 |
| syncCursor | String | Lichess: 마지막 gameId, Chess.com: "2024/03" |
| totalFetched | int | 저장된 게임 수 누계 |
| errorMsg | String | 실패 시 오류 메시지 |
| atomicFetched | AtomicInteger | Chess.com 병렬 처리용 thread-safe 카운터 |

**주요 메서드**

| 메서드 | 설명 |
|--------|------|
| `create(userId, platform, platformUsername)` | 팩토리, PENDING 상태로 생성 |
| `start()` | IN_PROGRESS로 전환 |
| `complete()` | COMPLETED, atomicFetched → totalFetched 동기화 |
| `fail(errorMsg)` | FAILED |
| `expireToken()` | TOKEN_EXPIRED |
| `progress(cursor, count)` | Lichess 순차 처리용 cursor + count 갱신 |
| `progressAtomic(cursor, count)` | Chess.com 병렬 처리용 thread-safe 갱신 |

#### `sync/SyncJobRepository.java`

```java
Optional<SyncJob> findById(Long id);
Optional<SyncJob> findLatestByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
SyncJob save(SyncJob job);
```

---

#### `game/GameResult.java`

```java
public enum GameResult { WIN, LOSS, DRAW, OTHER }
```

#### `game/Game.java`

수집된 게임 1건을 표현하는 도메인 객체. 양 플랫폼을 통합 스키마로 저장.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 서비스 사용자 ID |
| platform | OAuthPlatForm | LICHESS / CHESSCOM |
| platformGameId | String | Lichess gameId 또는 Chess.com 게임 URL |
| username | String | 플레이어 계정명 |
| opponentUsername | String | 상대방 계정명 |
| playerColor | String | "WHITE" 또는 "BLACK" |
| result | GameResult | WIN / LOSS / DRAW / OTHER |
| timeClass | String | blitz / bullet / rapid / classical |
| timeControl | String | "180+2" 등 (Chess.com만 존재) |
| rated | Boolean | 레이팅 게임 여부 |
| moves | String | PGN (Chess.com) 또는 moves 문자열 (Lichess) |
| variant | String | 게임 변형 (standard 등) |
| playedAt | LocalDateTime | 게임 종료 시각 (UTC) |
| createdAt | LocalDateTime | DB 저장 시각 |

#### `game/GameRepository.java`

```java
List<Game> saveAll(List<Game> games);
```

---

### 3.2 chessmate-infra-persistence

#### `game/entity/GameJpaEntity.java`

- 테이블명: `game`
- 인덱스: `idx_game_user_platform` (user_id, platform)
- Unique 제약: `uk_game_platform_id` (platform, platform_game_id) → 재수집 시 중복 방지

#### `game/jpaRepository/GameJpaRepository.java`

```java
// 중복 체크용: 이미 저장된 platformGameId 목록 조회
List<String> findExistingPlatformGameIds(OAuthPlatForm platform, List<String> ids);
```

#### `game/mapper/GameMapper.java`

`Game` ↔ `GameJpaEntity` 양방향 변환.

#### `game/repositoryImpl/GameRepositoryImpl.java`

`saveAll()` 내부 동작:
1. 입력 게임의 `platformGameId` 목록 추출
2. DB에서 이미 존재하는 ID 조회
3. 신규 게임만 필터링하여 bulk insert
4. 중복 스킵 수 로깅

---

#### `sync/entity/SyncJobJpaEntity.java`

- 테이블명: `sync_job`
- 인덱스: `idx_user_platform` (user_id, platform), `idx_status` (status)
- `platform_username VARCHAR(100)` 컬럼 포함 → **DB 마이그레이션 필요** (기존 테이블에 없을 경우)

---

### 3.3 chessmate-infra-redis

#### `sync/SyncQueueKey.java`

| 상수 | Redis 키 |
|------|----------|
| LICHESS | `queue:sync:lichess` |
| CHESSCOM | `queue:sync:chesscom` |

#### `sync/SyncJobProducer.java`

`enqueue(OAuthPlatForm, Long syncJobId)` → 플랫폼별 Redis List에 jobId push

#### `sync/SyncJobConsumer.java`

`poll(OAuthPlatForm)` → 큐에서 jobId를 즉시 꺼냄 (non-blocking).  
데이터 없으면 `Optional.empty()` 반환.

#### `token/PlatformTokenStore.java`

Worker가 Redis 키 구조를 직접 알 필요 없도록 `AuthRedisRepository`를 래핑한 파사드.

| 메서드 | 설명 |
|--------|------|
| `getLichessAccessToken(userId)` | Lichess Access Token 조회 |
| `saveLichessAccessToken(userId, token, ttl)` | 저장 (TTL 지정) |
| `getChesscomAccessToken(userId)` | Chess.com Access Token 조회 |
| `getChesscomRefreshToken(userId)` | Chess.com Refresh Token 조회 |
| `saveChesscomTokens(userId, access, refresh, ttl)` | Access TTL 지정, Refresh TTL 30일 고정 |

---

### 3.4 chessmate-external

#### `LichessApi.java`

```
GET /api/account
  - Authorization: Bearer {token}
  - 현재 사용자 계정 조회

GET /api/games/user/{username}
  - Accept: application/x-ndjson
  - params: max(최대100), until(cursor gameId), sort(dateDesc)
  - 반환: \n 구분 NDJSON 문자열
```

#### `ChesscomApi.java`

```
GET /pub/player/{username}/games/archives
  - 반환: 월별 아카이브 URL 목록

GET {archiveUrl}
  - Authorization: Bearer {token} (선택)
  - 반환: 해당 월 게임 목록
```

#### 주요 DTO

| 클래스 | 위치 | 설명 |
|--------|------|------|
| `LichessGamesDto` | `dto/game` | Lichess 게임 단건 (id, rated, perf, createdAt, winner, moves, players) |
| `Players` / `Player` / `LichessUser` | `dto/game` | Lichess 플레이어 정보 |
| `ChesscomMonthlyArchiveResponse` | `dto` | 월간 게임 목록 |
| `ChesscomGameResponse` | `dto/chesscom` | Chess.com 게임 단건 |
| `ChesscomGamePlayerInfo` | `dto/chesscom` | Chess.com 플레이어 정보 (username, rating, result) |

---

### 3.5 chessmate-worker

#### `SyncJobDispatcher.java`

`@Scheduled(fixedDelay=1000ms)` 로 두 개의 스케줄 메서드 실행:

- `pollLichess()` → Lichess 큐 폴링 → `LichessGameSyncWorker.process(job)`
- `pollChessCom()` → Chess.com 큐 폴링 → `ChessComGameSyncWorker.process(job)`

---

#### `LichessGameSyncWorker.java`

**처리 방식**: 순차 + Rate Limit

```
process(job)
  ├─ 토큰 없음 → expireToken()
  ├─ job.start()
  └─ while true:
       RATE_LIMITER.acquire()  ← 0.05 req/sec (분당 3회)
       lichessApi.getGames(token, max=100, until=cursor, sort=dateDesc)
       parseNdjson() → List<LichessGamesDto>
       필터: rated=true AND perf IN {blitz,bullet,rapid,classical}
       toGame() 변환 → gameRepository.saveAll()
       cursor = 마지막 게임 ID
       job.progress(cursor, savedCount)
       if size < 100: break
  └─ job.complete()
```

**DTO → Game 변환 규칙**

| Game 필드 | Lichess 출처 |
|-----------|-------------|
| platformGameId | `dto.id()` |
| playerColor | `players.white.user.name == username` ? WHITE : BLACK |
| opponentUsername | 상대편 user.name |
| result | winner=null→DRAW, winner색=playerColor→WIN, else→LOSS |
| timeClass | `dto.perf()` |
| moves | `dto.moves()` |
| variant | `dto.variant()` |
| playedAt | `dto.createdAt()` (epoch millis → UTC) |

---

#### `ChessComGameSyncWorker.java`

**처리 방식**: 토큰 유무에 따라 분기

```
process(job)
  ├─ job.start()
  ├─ getGameArchives(username) → 전체 아카이브 URL 목록
  ├─ filterPending(archives, cursor) → 미처리 URL만 추출
  │
  ├─(토큰 있음) processParallel()
  │    ExecutorService(parallelism=5)
  │    CompletableFuture.runAsync(fetchAndSave) × N
  │
  └─(토큰 없음) processSequential()
       FALLBACK_RATE_LIMITER (0.5 req/sec)
       fetchAndSave() 순차 실행
```

**fetchAndSave() 흐름**
```
getArchiveByUrl(url, authHeader)
  └─ 401 → ChesscomTokenRefresher.refresh() → 1회 재시도
           └─ refresh 실패 → job.expireToken()

필터: rated=true AND timeClass IN {blitz,bullet,rapid,classical}
toGame() 변환 → gameRepository.saveAll()
cursor = "yyyy/MM"
job.progressAtomic(cursor, savedCount)
```

**DTO → Game 변환 규칙**

| Game 필드 | Chess.com 출처 |
|-----------|---------------|
| platformGameId | `dto.getUrl()` |
| playerColor | white.username == username ? WHITE : BLACK |
| opponentUsername | 상대편 username |
| result | "win"→WIN, agreed/repetition/stalemate/…→DRAW, else→LOSS |
| timeClass | `dto.getTimeClass()` |
| timeControl | `dto.getTimeControl()` |
| moves | `dto.getPgn()` (전체 PGN) |
| variant | `dto.getRules()` |
| playedAt | `dto.getEndTime()` (epoch seconds → UTC) |

**Chess.com result 값 매핑**

| result 값 | GameResult |
|-----------|------------|
| `win` | WIN |
| `agreed`, `repetition`, `stalemate`, `insufficient`, `50move`, `timevsinsufficient`, `kingofthehill` | DRAW |
| `checkmated`, `resigned`, `timeout`, `abandoned`, 기타 | LOSS |

---

#### `ChesscomTokenRefresher.java`

Worker에서 401 감지 시 호출:
1. Redis에서 Refresh Token 조회
2. `oAuthService.refreshChesscomToken(refreshToken)` 호출
3. 새 토큰 Redis에 저장 후 Access Token 반환
4. 실패 시 `null` 반환 → Worker가 `TOKEN_EXPIRED` 처리

---

### 3.6 chessmate-api

#### `sync/SyncStatusController.java`

프론트엔드 폴링용 상태 조회 API.

```
GET /api/sync/status/{userId}?platform=LICHESS|CHESSCOM

Response:
{
  "jobId": 123,
  "status": "IN_PROGRESS",
  "totalFetched": 450,
  "syncCursor": "abc123gameId",
  "errorMsg": null
}
```

#### `global/auth/oauth/lichess/LichessOAuthService.java` (수정됨)

OAuth 콜백 처리 시:
```java
SyncJob job = SyncJob.create(userId, OAuthPlatForm.LICHESS, platformUsername);
syncJobRepository.save(job);
syncJobProducer.enqueue(OAuthPlatForm.LICHESS, job.getId());
```

#### `global/auth/oauth/chesscom/ChesscomOAuthService.java` (수정됨)

동일 패턴 적용.

---

## 4. 데이터 수집 전략

### Lichess

| 항목 | 값 |
|------|-----|
| 수집 방식 | 순차 (병렬 불가) |
| Rate Limit | 0.05 req/sec (분당 3회) |
| 페이지 크기 | 100게임/청크 |
| cursor 단위 | gameId (마지막으로 받은 게임의 ID) |
| 토큰 필수 여부 | 필수 (없으면 TOKEN_EXPIRED) |
| 필터 | rated=true, perf ∈ {blitz, bullet, rapid, classical} |

### Chess.com

| 항목 | 값 |
|------|-----|
| 수집 방식 | 토큰 있으면 병렬(parallelism=5), 없으면 순차 |
| Rate Limit (비토큰) | 0.5 req/sec |
| 단위 | 월별 아카이브 1개 = URL 1개 |
| cursor 단위 | "yyyy/MM" (마지막 완료 월) |
| 토큰 필수 여부 | 선택 (없으면 Public API fallback) |
| 필터 | rated=true, timeClass ∈ {blitz, bullet, rapid, classical} |
| 401 처리 | Refresh Token으로 자동 갱신 후 1회 재시도 |

---

## 5. game 테이블 DDL

```sql
CREATE TABLE game (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    platform          VARCHAR(20)  NOT NULL,
    platform_game_id  VARCHAR(255) NOT NULL,
    username          VARCHAR(100) NOT NULL,
    opponent_username VARCHAR(100),
    player_color      VARCHAR(10),
    result            VARCHAR(10),
    time_class        VARCHAR(20),
    time_control      VARCHAR(30),
    rated             BOOLEAN,
    moves             TEXT,
    variant           VARCHAR(30),
    played_at         DATETIME,
    created_at        DATETIME     NOT NULL,

    INDEX idx_game_user_platform (user_id, platform),
    UNIQUE KEY uk_game_platform_id (platform, platform_game_id)
);
```

> `sync_job` 테이블에 `platform_username VARCHAR(100) NOT NULL` 컬럼이 없는 경우 ALTER TABLE 필요:
> ```sql
> ALTER TABLE sync_job ADD COLUMN platform_username VARCHAR(100) NOT NULL DEFAULT '';
> ```

---

## 6. 미결 사항 및 다음 단계

### 즉시 필요

| 항목 | 내용 |
|------|------|
| DB 마이그레이션 | `game` 테이블 생성, `sync_job.platform_username` 컬럼 추가 |
| `GameTaskProducer.java` 삭제 | `chessmate-api`에 미사용 상태로 잔존 |

### 다음 구현 (집계 통계)

수집된 raw game 데이터를 기반으로 아래 집계 테이블 설계 및 구현 예정.

| 집계 항목 | 내용 | 기준 |
|-----------|------|------|
| 일별 스트릭 | 날짜별 게임 수, 총/승/무/패 | `played_at::date` GROUP BY |
| 색상별 승률 | 백/흑 각각 승·무·패 수, 승률 | `player_color` GROUP BY |
| 첫 수 통계 | 백/흑 각각 가장 많이 둔 첫 수 | `moves` 파싱 (첫 토큰 추출) |

> **첫 수 추출 방식**
> - Lichess: `moves` 문자열의 첫 공백 이전 토큰 (e.g., `"e4 e5 ..."` → `"e4"`)
> - Chess.com: PGN에서 `1. ` 이후 첫 수 파싱

### 증분 수집 (향후)

- `lastGameAt` 필드를 SyncJob 또는 별도 테이블에 보관
- 재수집 시 해당 일시 이후 게임만 요청 → API 호출 최소화
- Chess.com: 현재 월 아카이브부터 역방향으로 처음 이미 수집된 월에서 중단
- Lichess: `until` 파라미터로 cursor gameId 이전 게임만 요청 (현재도 지원됨)