# stat 테이블 수동 삭제 후 증분 수집으로 복구 불가 — 원인 분석 및 해결

작성일: 2026-04-14

---

## 1. 무슨 일이 일어난 것인가

`first_move_stat` 테이블을 수동 삭제(truncate/delete) 후
1분마다 실행되는 `ScheduledSyncTrigger`를 기다렸지만 데이터가 채워지지 않았다.

---

## 2. 왜 채워지지 않는가 — 근본 원인

### 핵심 설계 가정

현재 구조에서 `statAggregator.aggregate()`는 **신규 게임이 실제로 DB에 insert됐을 때만** 호출된다.

```
ChessComGameSyncWorker.process():
    int savedBefore = job.getTotalFetched();  // 새 job이므로 항상 0
    → processParallel() / processSequential()
    int newlySaved = job.getTotalFetched() - savedBefore;

    if (newlySaved > 0) {         ← 이 조건 때문에 재집계가 안 됨
        statAggregator.aggregate(userId, OAuthPlatForm.CHESSCOM);
    }

LichessGameSyncWorker.processIncremental():
    if (totalNewlySaved > 0) {    ← 동일 패턴
        statAggregator.aggregate(userId, OAuthPlatForm.LICHESS);
    }
```

### 증분 수집 트리거 동작 흐름

```
ScheduledSyncTrigger (매 1분)
  └─ SyncJob 생성 → Redis enqueue

SyncJobDispatcher (매 1초 폴링)
  └─ ChessComGameSyncWorker.process()
       └─ cursor = 전월 → filterPending → 당월 archive만 처리
            └─ gameRepository.saveAll(당월게임들)
                 → 이미 DB에 있음 → 신규 insert = 0건
                 → newlySaved = 0
                 → statAggregator 호출 안 됨
```

### 결론

> stat 테이블은 game 테이블의 **파생 집계 데이터**인데,  
> 재집계 트리거가 오직 **"신규 게임 수집"** 이벤트에만 연결돼 있다.  
> stat 테이블이 어떤 이유로든 비면, 복구 경로가 존재하지 않는다.

가정 자체가 틀렸다:  
"stat 테이블은 항상 game 테이블과 동기화되어 있다" → **보장되지 않는다**.

---

## 3. 동일한 문제가 발생할 수 있는 다른 상황들

| 상황 | 결과 |
|------|------|
| stat 테이블 수동 삭제 | 복구 불가 (이번 케이스) |
| `statAggregator.aggregate()` 내부에서 예외 발생 | 해당 플랫폼 stat 누락 — 다음 신규 게임이 들어올 때까지 방치 |
| 신규 게임이 장기간 없는 유저 | stat이 비어 있어도 재집계 기회 없음 |
| 코드 배포로 집계 로직이 변경됨 | 기존 stat 데이터가 새 로직과 불일치 — 수동 재집계 방법 없음 |

---

## 4. Chess.com API 불안정 시 데이터 정합성 위험

### 문제: 실패한 archive는 영원히 재시도되지 않는다

```
병렬 수집 중 특정 archive 응답 실패
  → catch(Exception) → log.warn → failCount++
  → job은 COMPLETED 처리됨

다음 증분 수집:
  cursor = 전월 → filterPending → 당월 archive만 처리
  → 실패했던 과거 archive는 다시 건드리지 않음
```

예시:
- 2024/10 archive 수집 중 타임아웃
- job COMPLETED 처리
- 다음 트리거: cursor = 2026/03 → 2026/04만 처리
- **2024/10 게임은 영구 누락**

### 문제: 초기 수집 실패 후 재시도가 전체 수집인지 확인 안 됨

```java
// ScheduledSyncTrigger.scheduleChesscomUsers()
if (lastJob.isPresent() && lastJob.get().getStatus() == SyncStatus.COMPLETED) {
    // 증분
} else {
    // 최초 또는 이전 실패: 전체 수집
}
```

FAILED 상태일 때는 전체 수집으로 재시도하므로 이 부분은 올바르다.  
단, **COMPLETED 상태지만 내부 archive 일부가 실패**한 경우는 감지 못한다.

---

## 5. 해결 방안

### 방안 A — 즉시 해결: stat 강제 재집계 Admin API 추가 (추천)

stat 재집계를 게임 수집과 독립적으로 트리거할 수 있는 내부 API.

```
POST /internal/admin/stat/recompute?userId={userId}&platform={platform}
```

구현 위치: `chessmate-worker` 또는 `chessmate-api` 내 admin 전용 컨트롤러

```java
// 예시 구현
@PostMapping("/internal/admin/stat/recompute")
public void recompute(@RequestParam Long userId,
                      @RequestParam OAuthPlatForm platform) {
    statAggregator.aggregate(userId, platform);
}
```

장점:
- 구현 단순
- stat 손상 시 즉시 복구 가능
- 배포 후 전체 재집계 시에도 활용 가능

---

### 방안 B — 구조적 해결: stat이 비어 있으면 강제 재집계

Worker가 stat 재집계 전 해당 유저의 stat 존재 여부를 확인.

```java
// ChessComGameSyncWorker.process() 수정
boolean statEmpty = firstMoveStatRepository
    .countByUserIdAndPlatform(userId, OAuthPlatForm.CHESSCOM) == 0;

if (newlySaved > 0 || statEmpty) {
    statAggregator.aggregate(userId, OAuthPlatForm.CHESSCOM);
}
```

장점:
- 증분 수집 파이프라인 자체에서 자동 복구
- 배포 없이도 점진적 복구

단점:
- 매 증분 수집마다 stat 테이블 count 쿼리 추가 (경미한 오버헤드)
- 어떤 stat 테이블을 기준으로 삼을지 결정 필요

---

### 방안 C — 데이터 정합성: 실패 archive 추적 및 재시도

현재 archive 단위 실패가 조용히 묻힌다. 실패를 추적하고 재시도하는 구조 추가.

```
failed_sync_archive 테이블:
  - user_id
  - platform
  - archive_url
  - failed_at
  - retry_count
```

`ScheduledSyncTrigger`가 이 테이블을 조회해 재시도 enqueue.

장점: 게임 누락 없이 완전한 데이터 수집 가능  
단점: 구현 복잡도 증가 — 현재 우선순위에서 낮을 수 있음

---

## 6. 권장 적용 순서

| 우선순위 | 방안 | 이유 |
|---------|------|------|
| 즉시 | **방안 A** (Admin API) | 현재 빈 stat 테이블 복구 |
| 단기 | **방안 B** (stat 비어있으면 강제 재집계) | 동일 문제 재발 방지 |
| 장기 | **방안 C** (archive 실패 추적) | API 불안정 시 게임 누락 방지 |

---

## 7. 현재 상태에서 즉시 복구하는 방법

방안 A 구현 전 긴급 복구:

**옵션 1**: 해당 유저의 SyncJob을 FAILED 상태로 수동 update
```sql
UPDATE sync_job
SET status = 'FAILED'
WHERE user_id = {userId}
  AND platform = 'CHESSCOM'
  AND status = 'COMPLETED'
ORDER BY created_at DESC
LIMIT 1;
```
→ 다음 스케줄러 실행 시 전체 수집 모드로 fallback → stat 재집계 실행

**옵션 2**: `statAggregator.aggregate()`를 직접 호출하는 임시 테스트/관리 엔드포인트 추가 후 제거