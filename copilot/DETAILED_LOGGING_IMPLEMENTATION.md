# 배치 중복 게임 추적을 위한 상세 로깅 구현 보고서

## 개요
2월 13일 게임이 중복으로 저장되는 문제를 추적하기 위해 배치의 각 단계(Reader → Processor → Writer → 검증)에서 상세한 로깅을 추가했습니다.

---

## 구현된 로깅 포인트

### 1️⃣ Reader 단계 (LichessNdjsonItemReader)
**파일**: `chessmate-worker/src/main/java/com/chessmate/worker/batch/reader/LichessNdjsonItemReader.java`

**추가 내용**:
- 게임 카운터 추가: `gameCount` 변수로 읽어온 게임 총 개수 추적
- 각 게임마다 로깅:
  ```
  [Batch-Reader] [GAME-READ] gameCount=1, gameId=GZS0XKpO, createdAt=1770941828110, lastMoveAt=1770942114794
  [Batch-Reader] [GAME-READ] gameCount=2, gameId=61pj6Zk9, createdAt=1701319685433, lastMoveAt=1701320500056
  ```
- 스트림 완료 시 총 개수 출력:
  ```
  [Batch-Reader] [COMPLETE] 게임 조회 완료 - batchId=xxx, username=mg0922, totalCount=81
  ```

**효과**: API에서 몇 개의 게임을 반환했는지 확인 가능

---

### 2️⃣ Processor 단계 (LichessGameProcessor)
**파일**: `chessmate-worker/src/main/java/com/chessmate/worker/batch/processer/LichessGameProcessor.java`

**추가 내용**:
- 각 게임 처리 시작:
  ```
  [Game-Process] gameId=GZS0XKpO, createdAt=1770941828110, lastMoveAt=1770942114794, username=mg0922, perf=blitz
  ```

- 지원되지 않는 게임 타입 필터링 로그:
  ```
  지원되지 않는 게임 타입입니다. perf='correspondence', username='mg0922', gameId='xxx', createdAt='1701319685433' - 이 게임은 건너뜁니다.
  ```

- 최종 처리 결과:
  ```
  [Game-Process] [SUCCESS] gameId=GZS0XKpO, result=WIN, gameType=BLITZ, date=2026-02-15
  ```

**효과**: 어떤 게임이 처리되었고, 어떤 게임이 필터링되었는지 추적 가능

---

### 3️⃣ Writer 단계 (GameStatItemWriter)
**파일**: `chessmate-worker/src/main/java/com/chessmate/worker/batch/write/GameStatItemWriter.java`

**추가 내용**:
- 집계된 스트릭 데이터 상세 로깅:
  ```
  [Batch-Writer] [AGGREGATED-DATA] 집계된 일일 스트릭 목록:
    - userId=1, date=2026-02-15, win=1, lose=0, draw=0, lastGameAt=1770942114794, lastRating=1274
    - userId=1, date=2026-02-13, win=1, lose=1, draw=0, lastGameAt=1594443520656, lastRating=1246
  ```

**효과**: DB에 저장되기 전 최종 집계 데이터 확인 가능

---

### 4️⃣ 배치 완료 후 검증 (UserBatchServiceImpl)
**파일**: `chessmate-worker/src/main/java/com/chessmate/worker/batch/service/UserBatchServiceImpl.java`

**추가 내용**:
- 배치 완료 후 실제 DB 데이터 조회:
  ```
  [Batch-Trigger] [DB-VERIFY] 저장된 스트릭 데이터 검증 중...
  [Batch-Trigger] [DB-DATA] 저장된 스트릭 총 100개
    - date=2026-02-15, win=1, lose=0, draw=0, lastGameAt=1770942114794, lastRating=1274
    - date=2026-02-14, win=2, lose=1, draw=0, lastGameAt=1770854200000, lastRating=1300
    - date=2026-02-13, win=1, lose=1, draw=0, lastGameAt=1594443520656, lastRating=1246
    ...
  ```

**효과**: 배치 완료 후 실제 저장된 데이터를 즉시 확인 가능

---

## 추가된 Repository 메서드

### UserDailyStreakRepositoryImpl
**메서드**: `findByUserId(Long userId)`
```java
public List<UserDailyStreak> findByUserId(Long userId) {
    return jpaRepository.findAllByUserId(userId).stream()
        .map(UserDailyStreakMapper::toDomain)
        .toList();
}
```

### UserDailyStreakJpaRepository
**쿼리**: `findAllByUserId(Long userId)`
```java
List<UserDailyStreakEntity> findAllByUserId(Long userId);
```

---

## 로그 분석 시나리오

### ✅ 정상 동작 시나리오
```
[Batch-Reader] [START] 게임 데이터 조회 시작 - since=1594134084204
[Batch-Reader] [GAME-READ] gameCount=1, gameId=NEW_GAME_1, createdAt=1770961111111, lastMoveAt=1770961222222
[Batch-Reader] [GAME-READ] gameCount=2, gameId=NEW_GAME_2, createdAt=1770962222222, lastMoveAt=1770962333333
[Batch-Reader] [COMPLETE] 게임 조회 완료 - totalCount=2

[Game-Process] [SUCCESS] gameId=NEW_GAME_1, result=WIN, date=2026-02-15
[Game-Process] [SUCCESS] gameId=NEW_GAME_2, result=LOSE, date=2026-02-15

[Batch-Writer] [AGGREGATED-DATA] 집계된 일일 스트릭 목록:
  - userId=1, date=2026-02-15, win=1, lose=1, draw=0, lastGameAt=1770962333333, lastRating=1300

[Batch-Trigger] [DB-DATA] 저장된 스트릭 총 101개
  - date=2026-02-15, win=1, lose=1, draw=0, lastGameAt=1770962333333, lastRating=1300
```

→ **결론**: API 2개 반환 → Processor 2개 처리 → Writer에 2개 집계 → DB 최종 업데이트 완료

---

### ❌ 중복 게임 시나리오 (문제 상황)
```
[Batch-Reader] [START] 게임 데이터 조회 시작 - since=1594134084203  (← +1 안됨!)
[Batch-Reader] [GAME-READ] gameCount=1, gameId=LAST_GAME, createdAt=1594134084203, lastMoveAt=1594134084203  (← 중복!)
[Batch-Reader] [GAME-READ] gameCount=2, gameId=NEW_GAME, createdAt=1594134084204, lastMoveAt=1594134084204

[Game-Process] [SUCCESS] gameId=LAST_GAME, result=WIN, date=2020-07-08
[Game-Process] [SUCCESS] gameId=NEW_GAME, result=LOSE, date=2020-07-09

[Batch-Writer] [AGGREGATED-DATA] 집계된 일일 스트릭 목록:
  - userId=1, date=2020-07-08, win=1, lose=0, draw=0, lastGameAt=1594134084203, lastRating=1246
  - userId=1, date=2020-07-09, win=0, lose=1, draw=0, lastGameAt=1594134084204, lastRating=1240

[Batch-Trigger] [DB-DATA] 저장된 스트릭 총 100개
  - date=2020-07-08, win=2, lose=0, draw=0, lastGameAt=1594134084203, lastRating=1246  (← 증가!)
```

→ **결론**: since가 정확하지 않아 마지막 게임이 다시 조회되고 win이 +1 증가

---

## 배포 단계

1. ✅ `LichessNdjsonItemReader.java` 수정 완료
2. ✅ `LichessGameProcessor.java` 수정 완료
3. ✅ `GameStatItemWriter.java` 수정 완료
4. ✅ `UserBatchServiceImpl.java` 수정 완료
5. ✅ `UserDailyStreakRepositoryImpl.java` 수정 완료
6. ✅ `UserDailyStreakJpaRepository.java` 수정 완료

## 다음 단계

1. **프로젝트 빌드**:
   ```bash
   ./gradlew clean build
   ```

2. **이전 BATCH_SINCE_PARAMETER_FIX_REPORT의 수정사항 함께 반영**:
   - `since = lastGameAt + 1` 적용 필수

3. **스테이징 환경에서 테스트**:
   - mg0922 사용자로 로그인
   - 게임 몇 개 플레이
   - 배치 수동 트리거
   - 로그 확인

4. **로그에서 확인할 사항**:
   - Reader에서 읽어온 게임 수
   - Processor에서 처리된 게임 수
   - Writer에서 집계된 게임 수 및 날짜별 데이터
   - DB 최종 저장 데이터 (2월 13일이 1개 또는 2개인지)

---

## 예상 효과

- ✅ API에서 반환하는 게임 개수 명확 파악
- ✅ 각 게임의 ID, 시간 정보로 중복 여부 판단
- ✅ 필터링된 게임(correspondence 등) 확인
- ✅ 최종 DB 상태 즉시 확인
- ✅ 문제 발생 시 로그를 통해 단계별 추적 가능

