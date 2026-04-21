# Bug Report: 신규 유저 `user_perf_stat` 미업데이트

## 요약

| 테이블 | 증분 수집 후 상태 |
|--------|----------------|
| `game` | ✅ 정상 저장 |
| `user_daily_game_stat` (스트릭) | ✅ 정상 저장 |
| `user_color_stat` | ✅ 정상 저장 |
| `user_first_move_stat` | ✅ 정상 저장 |
| `user_perf_stat` (레이팅·경기수) | ❌ 저장 안됨 |

---

## 전체 흐름

### 30분 스케줄러 파이프라인

```
ScheduledSyncTrigger (매 30분)
  └─ 전체 유저 조회
       ├─ [Lichess]   SyncJob 생성 (cursor 없음)   → Redis 큐
       └─ [Chess.com] SyncJob 생성 (cursor=전월)   → Redis 큐

SyncJobDispatcher (1초 폴링)
  └─ 큐에서 SyncJob 꺼냄
       ├─ LichessGameSyncWorker.process()
       └─ ChessComGameSyncWorker.process()
```

---

### Lichess 수집 분기

```
LichessGameSyncWorker.process()
  │
  ├─ DB에 게임 없음 → processFullSync()
  │    └─ (항상) aggregate() + fetch() 호출
  │
  └─ DB에 게임 있음 → processIncremental()
       └─ (조건부) aggregate() + fetch() 호출
            └─ 조건: totalNewlySaved > 0
                      OR isAnyStatEmpty() == true
```

### Chess.com 수집 분기

```
ChessComGameSyncWorker.process()
  │
  ├─ 이전 완료 이력 없음 → cursor=null → 전체 아카이브 수집
  │
  └─ 이전 완료 이력 있음 → cursor=전월 → 당월 아카이브만 수집
       └─ (조건부) aggregate() + fetch() 호출
            └─ 조건: newlySaved > 0
                      OR isAnyStatEmpty() == true
```

---

### aggregate() + fetch() 내부 흐름

```
statAggregator.aggregate(userId, platform)
  │
  ├─ game 테이블 전체 로드
  ├─ user_daily_game_stat 재계산 → 저장  ← 스트릭 데이터 여기서 생성
  ├─ user_color_stat 재계산 → 저장
  └─ user_first_move_stat 재계산 → 저장

perfStatFetcher.fetch(userId, platform, username)   ← 이 단계가 문제
  │
  ├─ [Lichess]   lichessApi.getUser(username)
  │                → perfs.bullet/blitz/rapid/classical.rating
  │                → user_color_stat에서 게임수 합산
  │                → user_perf_stat 저장
  │
  └─ [Chess.com] chesscomApi.getPlayerStats(username)
                   → chessBullet/chessBlitz/chessRapid.last().rating
                   → record.win/loss/draw
                   → user_perf_stat 저장
```

---

## 버그 1: fetch()의 예외 무음 처리

`PerfStatFetcher.java:42`

```java
@Transactional
public void fetch(Long userId, OAuthPlatForm platform, String username) {
    try {
        if (platform == OAuthPlatForm.LICHESS) {
            fetchLichess(userId, username);   // 외부 API 호출
        } else {
            fetchChesscom(userId, username);  // 외부 API 호출
        }
    } catch (Exception e) {
        log.error("...");
        // 예외를 삼키고 그냥 리턴
        // user_perf_stat는 빈 상태로 남음
    }
}
```

**결과**: `aggregate()`는 이미 성공했기 때문에 스트릭·컬러·첫수 통계는 정상 저장됨.  
`fetch()`만 조용히 실패 → `user_perf_stat` 미저장. 로그에는 에러가 찍히지만 호출부에서 알 방법이 없음.

---

## 버그 2: isAnyStatEmpty()가 user_perf_stat를 포함하지 않음

`GameStatAggregator.java:53`

```java
public boolean isAnyStatEmpty(Long userId, OAuthPlatForm platform) {
    return !firstMoveStatRepository.existsByUserIdAndPlatform(userId, platform)
        || !colorStatRepository.existsByUserIdAndPlatform(userId, platform)
        || !dailyStatRepository.existsByUserIdAndPlatform(userId, platform);
        // user_perf_stat 체크 없음
}
```

이 메서드는 "stat 테이블 중 하나라도 비어있으면 강제 재집계"를 위한 안전망인데,  
`user_perf_stat`가 빠져 있어 `fetch()` 실패 상황을 감지하지 못함.

**결과**:
- `fetch()` 1회 실패 → `user_perf_stat` 비어 있음
- 다음 증분 스케줄 실행 시:
  - `newlySaved = 0` (신규 게임 없음)
  - `isAnyStatEmpty = false` (나머지 3개 테이블은 정상)
  - 조건 `false || false = false` → `fetch()` 재호출 없음
  - `user_perf_stat` 영구 빈 상태

---

## 두 버그가 겹치는 시나리오 (신규 유저)

### 타임라인

```
T+0분   유저 가입 (체스 플랫폼에 게임 0판)

T+30분  첫 스케줄러 실행
         Lichess  → processFullSync() → 게임 0개 수집
                   aggregate() → 게임 없음, 조기 리턴
                   fetch()     → API 응답: 모든 타임클래스 games=0
                                 → 조건 (games==0 && perf.games()==0) 모두 충족
                                 → user_perf_stat에 저장 없음 (정상)
         Chess.com → cursor=null 전체 수집 → 게임 0개
                   isAnyStatEmpty=true → aggregate() + fetch() 호출
                   fetch() → API: chessBullet/Blitz/Rapid 모두 null
                            → user_perf_stat에 저장 없음 (정상)
         → 두 플랫폼 모두 SyncJob.status = COMPLETED 저장

T+30~60분  유저가 게임을 두기 시작

T+60분  두 번째 스케줄러 실행
         Lichess  → DB에 게임 없음 → 여전히 processFullSync()
                   게임 수집 성공 → game 테이블 저장 ✅
                   aggregate() → user_daily_game_stat (스트릭) 저장 ✅
                   fetch() → 외부 API 실패 또는 데이터 미반영
                            → catch(Exception e) { log.error() }
                            → user_perf_stat 미저장 ❌

         Chess.com → 이전 COMPLETED 있음 → cursor=전월 → 증분
                   당월 아카이브에서 신규 게임 수집
                   newlySaved > 0 → aggregate() + fetch() 호출
                   game 테이블 저장 ✅, 스트릭 ✅
                   fetch() → 외부 API 실패 → user_perf_stat ❌

T+90분  세 번째 스케줄러 실행 (신규 게임 없음 가정)
         Lichess  → DB에 게임 있음 → processIncremental()
                   newlySaved = 0
                   isAnyStatEmpty → first_move ✅, color ✅, daily ✅ → false
                   조건: 0 > 0 || false = false → fetch() 호출 안됨
                   → user_perf_stat 계속 빈 상태 ❌

         Chess.com → 동일한 이유로 fetch() 재호출 없음 ❌
```

---

## 수정 방법

### Fix 1: isAnyStatEmpty()에 user_perf_stat 추가

`GameStatAggregator.java`

```java
public boolean isAnyStatEmpty(Long userId, OAuthPlatForm platform) {
    return !firstMoveStatRepository.existsByUserIdAndPlatform(userId, platform)
        || !colorStatRepository.existsByUserIdAndPlatform(userId, platform)
        || !dailyStatRepository.existsByUserIdAndPlatform(userId, platform)
        || !perfStatRepository.existsByUserIdAndPlatform(userId, platform); // 추가
}
```

`UserPerfStatRepository`에 메서드 추가:

```java
boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
```

**효과**: `fetch()` 실패 → `user_perf_stat` 비어있음 → 다음 스케줄에서 `isAnyStatEmpty=true` → 자동 재시도.

---

### Fix 2: fetch() 실패 시 재시도 가능하도록 예외 전파

`PerfStatFetcher.java`

현재 구조상 `fetch()`가 예외를 삼키지 않으면, 호출부의 `try-catch`가 잡아서 job을 FAILED로 표시하게 됨.  
Fix 1만으로도 충분하지만, 로깅을 강화해서 실패 원인을 추적하기 쉽게 만드는 것을 권장.

```java
public void fetch(Long userId, OAuthPlatForm platform, String username) {
    try {
        ...
    } catch (Exception e) {
        log.error("[PerfStatFetcher] 페프 스탯 조회 실패 userId={} platform={} username={} error={}",
            userId, platform, username, e.getMessage(), e);
        // Fix 1이 적용되면 다음 스케줄에서 isAnyStatEmpty=true로 재시도됨
    }
}
```

---

## 핵심 정리

| 원인 | 증상 |
|------|------|
| `fetch()` 예외 무음 처리 | game·스트릭은 저장되지만 user_perf_stat만 누락 |
| `isAnyStatEmpty()`에 user_perf_stat 미포함 | 1회 실패 후 신규 게임 없으면 영구 미복구 |

**최소 수정**: `isAnyStatEmpty()`에 `perfStatRepository.existsByUserIdAndPlatform()` 한 줄 추가.
