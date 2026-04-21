# Worker 스케줄러 흐름 정리

## 전체 구조

```
ScheduledSyncTrigger (cron 30분)
        │
        │  Redis 큐 enqueue (Lichess / Chess.com 분리)
        ▼
  [Redis Queue]
  lichess:sync:queue
  chesscom:sync:queue
        │
        │  fixedDelay 1초 polling
        ▼
  SyncJobDispatcher
        │
        ├──▶ LichessGameSyncWorker.process()
        └──▶ ChessComGameSyncWorker.process()
```

---

## 1. ScheduledSyncTrigger

**실행 주기**: `cron = "0 0/30 * * * *"` (매 시각 :00 / :30)

### enqueue 로직

30분 트리거가 발동되면 Lichess / Chess.com 각각 아래 흐름으로 실행됩니다.

#### 공통: 중복 enqueue 방지

```
staleThreshold = now - 60분

플랫폼당 DB 쿼리 1번 →
  findActiveUserIdsByPlatform(platform, staleThreshold)
  : updatedAt >= staleThreshold 인 PENDING/IN_PROGRESS 잡의 userId Set 반환

for (user : allUsers)
  if activeUserIds.contains(userId) → skip  (현재 처리 중)
  else → enqueue
```

- **updatedAt 기준**: 잡이 생성된 시각이 아닌 마지막으로 상태가 변경된 시각 기준
  - 예) 65분 전 생성 → 5분 전 IN_PROGRESS 전환 → stale 아님, 올바르게 skip
- **60분 threshold**: 60분 이상 PENDING/IN_PROGRESS 상태가 유지되면 worker 크래시로 간주 → 재 enqueue 허용

#### Lichess enqueue

active userId 체크 후 통과하면 cursor 없이 `SyncJob.create()` → enqueue.  
Worker가 DB 상태를 보고 전체/증분 수집을 자동 결정.

#### Chess.com enqueue

active userId 체크 후 통과하면 최신 잡 status를 확인해 cursor 결정.

```
플랫폼당 DB 쿼리 1번 →
  findLatestStatusByPlatform(CHESSCOM)
  : userId → SyncStatus Map (userId당 MAX(id) 잡 기준)

latestStatus == COMPLETED → cursor = 전월(yyyy/MM)  → 당월 아카이브만 재수집 (증분)
latestStatus != COMPLETED → cursor = null           → 전체 아카이브 수집 (초기/재시도)
```

#### 30분 트리거당 총 DB 쿼리 수

| 쿼리 | 횟수 |
|---|---|
| Lichess active userId 조회 | 1 |
| Chess.com active userId 조회 | 1 |
| Chess.com 최신 status Map 조회 | 1 |
| 루프 내 per-user 쿼리 | 0 (제거) |

---

## 2. SyncJobDispatcher

**실행 주기**: `fixedDelay = 1000ms` (처리 완료 후 1초 대기, 플랫폼당 독립)

Redis 큐를 RPOP으로 polling하여 jobId를 꺼낸 뒤 해당 Worker의 `process()`를 **동기 호출**.  
처리 중에는 다음 poll이 블로킹되므로, 한 번에 플랫폼당 1개의 잡이 처리됩니다.

---

## 3. LichessGameSyncWorker

### 전체 수집 (최초 로그인 / 이전 게임 없음)

```
while true:
  RateLimiter.acquire()          ← 0.3 req/s (18 req/min, 비인증 한도 20 req/min 이내)
  getGames(until=cursor, max=300, order=dateDesc)
  └─ 필터링 (rated, timeClass, createdAt 유효)
  └─ saveAll (platformGameId 기준 중복 제거)
  cursor = 마지막 게임 createdAt - 1ms
  games.size() < 300 이면 break
```

- `CHUNK_SIZE = 300` (Lichess API max)
- 역방향(dateDesc) 페이징으로 과거 게임까지 수집

### 증분 수집 (이후 30분 스케줄)

```
since = DB max(played_at) + 1ms
while true:
  RateLimiter.acquire()
  getGames(since=since, max=300, order=dateAsc)
  └─ saveAll
  since = 마지막 게임 createdAt + 1ms
  games.size() < 300 이면 break

신규 게임 있거나 stat 비어 있으면 → 통계 재집계
```

- API 레벨에서 `since` 파라미터로 필터링 → 불필요한 API 호출 없음

### 토큰 없는 유저

OAuth 토큰 없이 공개 API로 수집. Authorization 헤더 생략.  
RateLimiter(0.3 req/s)가 비인증 한도(20 req/min) 이내이므로 안전.

---

## 4. ChessComGameSyncWorker

### 아카이브 기반 수집

Chess.com 게임은 월별 아카이브 단위로 제공됨.

```
getGameArchives(username) → 전체 아카이브 URL 목록

filterPending(archives, cursor):
  cursor = "yyyy/MM" (전월)이면 해당 월 이후 URL만 반환
  cursor = null이면 전체 반환

토큰 있음 → processParallel (parallelism=5, RateLimiter 2.0 req/s)
토큰 없음 → processSequential (RateLimiter 0.5 req/s)
```

### 증분 수집

- cursor = 전월(`yyyy/MM`) → `filterPending`이 당월 아카이브 1건만 반환
- Chess.com 아카이브는 월 단위로 확정(immutable)이므로 이전 달은 재수집 불필요
- `saveAll` 내 `platformGameId` 중복 체크로 당월 기수집 게임 자동 스킵

### 전체 수집

- cursor = null → 전체 아카이브 수집
- 최초 로그인 또는 이전 잡이 COMPLETED가 아닌 경우

### 429 / 401 처리

- `429` → 해당 archive skip (job 전체는 계속)
- `401` → 토큰 갱신 시도 → 실패 시 job을 `TOKEN_EXPIRED`로 마킹

---

## SyncJob 상태 전이

```
PENDING → IN_PROGRESS → COMPLETED
                      → FAILED
                      → TOKEN_EXPIRED
```

| 상태 | 의미 |
|---|---|
| PENDING | Redis 큐에 enqueue 완료, 아직 처리 전 |
| IN_PROGRESS | Worker가 process() 시작 |
| COMPLETED | 수집 성공 |
| FAILED | 예외 발생 |
| TOKEN_EXPIRED | 토큰 갱신 실패 |

> 60분 이상 PENDING/IN_PROGRESS 유지 시 다음 트리거 사이클에서 stale로 간주,  
> 해당 유저는 재 enqueue 허용 (크래시 자동 복구).