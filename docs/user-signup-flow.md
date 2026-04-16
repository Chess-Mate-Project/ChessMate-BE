# 사용자 가입 이후 처리 흐름

> ChessMate-BE v2 기준 · 작성일 2026-04-07

---

## 목차

1. [전체 흐름 개요](#1-전체-흐름-개요)
2. [OAuth 콜백 처리](#2-oauth-콜백-처리)
3. [신규 / 기존 사용자 분기](#3-신규--기존-사용자-분기)
4. [SyncJob 생성 및 Redis 큐 등록](#4-syncjob-생성-및-redis-큐-등록)
5. [Worker 게임 수집 처리](#5-worker-게임-수집-처리)
6. [통계 재계산](#6-통계-재계산)
7. [동시 가입 시나리오](#7-동시-가입-시나리오)
8. [상태 다이어그램 요약](#8-상태-다이어그램-요약)
9. [플랫폼별 비교](#9-플랫폼별-비교)

---

## 1. 전체 흐름 개요

```
[프론트엔드]   [API 서버]                  [Redis]     [Worker]       [DB]
     │               │                        │              │             │
     │ OAuth 로그인 시작 ──────────────────────►│              │             │
     │               │                        │              │             │
     │◄──── 플랫폼 redirect ──────────────────│              │             │
     │               │                        │              │             │
     │ code + state ─►│                        │              │             │
     │               ├── 토큰 발급 (외부 API) ──────────────────────────────│
     │               ├── 사용자 조회 / 생성 ──────────────────────────────►│
     │               │                        │              │             │
     │               │   [신규 사용자일 때만]   │              │             │
     │               ├── SyncJob 생성 ────────────────────────────────────►│
     │               ├── Redis 큐 enqueue ────►│              │             │
     │               │                        │              │             │
     │◄── JWT 쿠키 ──│                        │              │             │
     │               │                        │              │             │
     │               │                        │◄─ 1초마다 poll ─┤             │
     │               │                        │              ├── 게임 수집 ─►│
     │               │                        │              ├── 통계 집계 ─►│
     │               │                        │              │             │
```

---

## 2. OAuth 콜백 처리

### 엔드포인트

| 플랫폼 | 콜백 URL |
|--------|----------|
| Chess.com | `GET /login/oauth2/code/chesscom?code=...&state=...` |
| Lichess | `GET /login/oauth2/code/lichess?code=...&state=...` |

### Chess.com 처리 순서

```
1. Redis에서 code_verifier 조회 (PKCE 검증)
       ↓
2. Chess.com API에서 Access/Refresh Token 발급
       ↓
3. ID Token 파싱 → userId(chesscomId), username 추출
       ↓
4. DB 조회 또는 신규 생성 (findByChesscomId)
       ↓
5. Access/Refresh Token → Redis 저장
       ↓
6. isNewUser 판별 → SyncJob 생성 (신규만)
       ↓
7. JWT 생성 → HttpOnly 쿠키로 응답
```

### Lichess 처리 순서

Chess.com과 동일하나 다음 차이점 있음:

- Refresh Token 없음 → Access Token만 Redis 저장
- ID Token 파싱 대신 Lichess API 직접 호출로 사용자 정보 조회 (`/api/account`)

---

## 3. 신규 / 기존 사용자 분기

```java
// ChesscomOAuthService.callback() 핵심 로직
ChesscomUser user = chesscomUserRepository
    .findByChesscomId(chesscomId)
    .orElseGet(() -> ChesscomUser.builder()
        .chesscomId(chesscomId)
        .username(username)
        .createdAt(LocalDateTime.now())
        .build());          // id = null (신규)

boolean isNewUser = user.getId() == null;

ChesscomUser saved = chesscomUserRepository.save(user);

if (isNewUser) {
    SyncJob job = SyncJob.create(saved.getId(), OAuthPlatForm.CHESSCOM, username);
    SyncJob savedJob = syncJobRepository.save(job);
    syncJobProducer.enqueue(OAuthPlatForm.CHESSCOM, savedJob.getId());
}
```

**판별 기준:** `user.getId() == null`

| 상황 | id 값 | 처리 |
|------|-------|------|
| 신규 가입 | null (아직 저장 전) | SyncJob 생성 + 큐 등록 |
| 재로그인 | DB에서 조회된 값 | SyncJob 생성 없음 |

---

## 4. SyncJob 생성 및 Redis 큐 등록

### SyncJob 초기 상태

```java
SyncJob.create(userId, platform, platformUsername)
// → status = PENDING
// → totalFetched = 0
// → syncCursor = null  (처음부터 수집)
```

### Redis 큐 구조

```
queue:sync:lichess   ← Lichess SyncJob ID들 (List)
queue:sync:chesscom  ← Chess.com SyncJob ID들 (List)
```

**연산:**
- 등록: `LPUSH queue:sync:{platform} {jobId}`
- 소비: `RPOP queue:sync:{platform}` (FIFO, non-blocking)

### SyncJob 상태 머신

```
          가입
           ↓
        PENDING
           ↓ Worker 실행
      IN_PROGRESS
        ↙   ↓   ↘
   FAILED  COMPLETED  TOKEN_EXPIRED
```

---

## 5. Worker 게임 수집 처리

### SyncJobDispatcher (스케줄러)

```java
@Scheduled(fixedDelay = 1000)   // 1초마다
public void pollLichess() {
    consumer.poll(LICHESS)
        .flatMap(syncJobRepository::findById)
        .ifPresent(lichessWorker::process);
}

@Scheduled(fixedDelay = 1000)   // 1초마다 (독립 스레드)
public void pollChessCom() {
    consumer.poll(CHESSCOM)
        .flatMap(syncJobRepository::findById)
        .ifPresent(chessComWorker::process);
}
```

두 플랫폼 큐는 독립적으로 폴링됩니다. 동시 실행이 가능합니다.

---

### 5-1. Lichess Worker

```
process(job)
  ├─ 토큰 없음 → TOKEN_EXPIRED, 종료
  ├─ job.start() → IN_PROGRESS
  └─ while (게임 있을 때까지)
       ├─ RateLimiter.acquire()  (0.05 permits/sec = 분당 3회)
       ├─ 토큰 재확인 (중간 만료 대비)
       ├─ GET /api/games/user/{username}
       │   └─ 100개씩 NDJSON 청크
       ├─ 필터: rated=true + timeClass IN (blitz/bullet/rapid/classical)
       ├─ gameRepository.saveAll()
       ├─ cursor 업데이트 (마지막 gameId)
       └─ syncJobRepository.save()  ← 중단점 저장
     ↓
  job.complete() → statAggregator.aggregate()
```

**특징:**
- 순차 처리 전용 (병렬 불가)
- `syncCursor` = 마지막 수집 게임 ID → 재시작 시 중단점부터 재개
- 토큰 없으면 즉시 `TOKEN_EXPIRED` 처리

---

### 5-2. Chess.com Worker

```
process(job)
  ├─ job.start() → IN_PROGRESS
  ├─ 모든 아카이브 목록 조회 (public API)
  ├─ filterPending() → cursor 이후 아카이브만 추림
  │
  ├─ 토큰 있음 → processParallel()
  │   └─ CompletableFuture × parallelism(5)
  │       └─ 각 아카이브별 fetchAndSave()
  │
  └─ 토큰 없음 → processSequential()
      └─ RateLimiter.acquire() (0.5 permits/sec)
          └─ 각 아카이브별 fetchAndSave()

fetchAndSave(url)
  ├─ getArchiveByUrl(URI, "Bearer {token}")
  ├─ 401 발생 시 → ChesscomTokenRefresher.refresh() → 1회 재시도
  ├─ 필터: rated=true + timeClass IN (blitz/bullet/rapid/classical)
  ├─ gameRepository.saveAll()
  └─ job.progressAtomic("yyyy/MM", count)   ← thread-safe
```

**특징:**
- 토큰 있을 때 병렬 처리 (parallelism=5, 설정 가능)
- `syncCursor` = 마지막 완료 월 "yyyy/MM"
- 401 자동 갱신 (Refresh Token 사용) 후 1회 재시도
- 토큰 없어도 public API로 수집 가능 (Rate Limit 적용)

---

### 중단점 복구 메커니즘

| 플랫폼 | cursor 형식 | 복구 동작 |
|--------|------------|----------|
| Lichess | `{gameId}` | `untilGameId` 파라미터로 그 이전 게임부터 재개 |
| Chess.com | `"yyyy/MM"` | 해당 월 이후 아카이브만 처리 |

장애 발생 후 재시작 시 `PENDING` 상태로 재등록하면 cursor 이후부터 이어서 수집합니다.

---

## 6. 통계 재계산

게임 수집 완료 직후 `GameStatAggregator.aggregate()` 호출:

```
aggregate(userId, platform)
  ├─ gameRepository.findByUserIdAndPlatform()
  ├─ 일일 통계  → user_daily_game_stat  (날짜 × 총/승/무/패)
  ├─ 색상 통계  → user_color_stat       (timeClass × 색상 × 승/무/패)
  └─ 첫 수 통계 → user_first_move_stat  (timeClass × 색상 × 첫 수 × count)
```

**전략: Full Recompute**
- 기존 통계 전체 DELETE → 새로 계산한 결과 saveAll
- 증분 계산 없음 (단순하고 정합성 보장)

**첫 수 추출 방식:**
- Lichess: `moves.split("\\s+")[0 or 1]` (space-separated notation)
- Chess.com: PGN 파싱 → `\n\n` 이후 moves 섹션에서 수 번호 제거 후 추출

---

## 7. 동시 가입 시나리오

### 7-1. 서로 다른 플랫폼 동시 가입

동일 사람이 Chess.com과 Lichess에 거의 동시에 로그인하는 경우:

```
                ChesscomOAuthService        LichessOAuthService
T1              callback() 시작             callback() 시작
T2              findByChesscomId() → empty  findByLichessId() → empty
T3              ChesscomUser 생성           LichessUser 생성
T4              save() → id=1              save() → id=1    (각자 독립 테이블)
T5              SyncJob(CHESSCOM) 생성      SyncJob(LICHESS) 생성
T6              queue:sync:chesscom push    queue:sync:lichess push
T7              JWT(userId=1, CHESSCOM)     JWT(userId=1, LICHESS)
```

**결과:**
- `chesscom_users.id=1` / `lichess_users.id=1` 각각 독립 생성
- 두 SyncJob이 Redis 큐에 동시 등록
- Dispatcher의 두 스케줄러가 각각 독립 처리
- 게임 수집은 거의 동시에 진행됨 (플랫폼별 Worker 독립)

> **설계 원칙:** 플랫폼 사용자는 별개 도메인 엔티티입니다. ChesscomUser와 LichessUser는 통합 User 테이블 없이 각자 독립 관리됩니다.

---

### 7-2. 같은 플랫폼 중복 로그인 (재진입)

동일 Chess.com 계정으로 두 개의 요청이 동시에 들어오는 경우:

```
                스레드 A (Chess.com)         스레드 B (Chess.com)
T1              findByChesscomId(X) → empty  findByChesscomId(X) → empty
T2              ChesscomUser 생성 (id=null)   ChesscomUser 생성 (id=null)
T3              save() → id=1               save() → (중복 가능)
T4              isNewUser=true              isNewUser=true
T5              SyncJob 중복 생성?           SyncJob 중복 생성?
```

**완화 장치:**
- `chesscom_users` 테이블에 `chesscom_id` unique 제약 → DB 레벨에서 중복 삽입 차단
- 이미 있는 유저는 `findByChesscomId()` 에서 반환되어 `isNewUser=false` → SyncJob 미생성
- 동시 요청 극히 드문 케이스이나, unique 제약 + @Transactional 조합으로 방어

---

### 7-3. 동시 Game 저장 (Chess.com 병렬 처리 중)

Chess.com Worker가 병렬로 여러 아카이브를 동시 처리할 때:

```
Thread 1: fetchAndSave("2024/01") → gameRepository.saveAll()
Thread 2: fetchAndSave("2024/02") → gameRepository.saveAll()
Thread 3: fetchAndSave("2024/03") → gameRepository.saveAll()
```

**동시성 보장:**
- `game` 테이블에 `(platform, platform_game_id)` unique 제약 → 중복 게임 DB 레벨 차단
- `saveAll()` 내부에서 기존 `platform_game_id` 조회 후 신규만 INSERT (pre-check)
- `job.progressAtomic()` = `synchronized` 메서드로 cursor/count thread-safe 업데이트

---

## 8. 상태 다이어그램 요약

```
[가입]
  │
  ▼
SyncJob: PENDING ─────── Worker 실행 ──────► IN_PROGRESS
                                                  │
                              ┌───────────────────┼───────────────────┐
                              ▼                   ▼                   ▼
                          COMPLETED            FAILED          TOKEN_EXPIRED
                              │
                              ▼
                       GameStatAggregator
                       (통계 재계산 완료)
```

```
[토큰 수명]

Lichess Access Token  ─── OAuth 발급 시 TTL 설정 (Redis 자동 만료)
Chess.com Access Token ─── OAuth 발급 시 TTL 설정
Chess.com Refresh Token ── 30일 고정 TTL
JWT Access Token ──────── 설정 파일 application.yml 기준
JWT Refresh Token ─────── 설정 파일 application.yml 기준 + Redis 저장
```

---

## 9. 플랫폼별 비교

| 항목 | Lichess | Chess.com |
|------|---------|-----------|
| Refresh Token | 없음 | 있음 (30일) |
| 수집 방식 | 순차 (청크 100개) | 토큰 있으면 병렬(5), 없으면 순차 |
| Rate Limit | 0.05 req/sec | 0.5 req/sec (토큰 없을 때) |
| 수집 단위 | 게임 100개 단위 | 월별 아카이브 단위 |
| Cursor 형식 | `{gameId}` | `"yyyy/MM"` |
| 토큰 없을 때 | 즉시 TOKEN_EXPIRED | Public API로 계속 수집 가능 |
| 401 자동 갱신 | 해당 없음 | Refresh Token으로 자동 갱신 |
| 사용자 조회 키 | `lichess_id` (String) | `chesscom_id` (Long) |
| 게임 포맷 | NDJSON | JSON (아카이브별) |
| 첫 수 추출 | space-separated | PGN 파싱 |

---

## 관련 파일 위치

| 역할 | 파일 |
|------|------|
| Chess.com OAuth | `chessmate-api/.../oauth/chesscom/ChesscomOAuthService.java` |
| Lichess OAuth | `chessmate-api/.../oauth/lichess/LichessOAuthService.java` |
| SyncJob 도메인 | `chessmate-domain/.../sync/SyncJob.java` |
| Redis 큐 Producer | `chessmate-infra-redis/.../sync/SyncJobProducer.java` |
| Redis 큐 Consumer | `chessmate-infra-redis/.../sync/SyncJobConsumer.java` |
| Worker 스케줄러 | `chessmate-worker/.../worker/SyncJobDispatcher.java` |
| Lichess Worker | `chessmate-worker/.../worker/LichessGameSyncWorker.java` |
| Chess.com Worker | `chessmate-worker/.../worker/ChessComGameSyncWorker.java` |
| 토큰 갱신 | `chessmate-worker/.../worker/ChesscomTokenRefresher.java` |
| 통계 집계 | `chessmate-worker/.../worker/GameStatAggregator.java` |