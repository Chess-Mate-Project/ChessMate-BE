# 배치 작업 중복 조회 문제 해결 보고서

## 문제 분석

### 기존 문제점
- **증분 조회 시 `since` 값 설정 오류**: 마지막 게임의 `lastGameAt` 값을 그대로 `since`에 전달
- **결과**: 마지막 게임과 동일한 시간의 게임들이 다시 조회되는 중복 발생
- **예시**: 
  - 마지막 게임의 `lastMoveAt = 1770942114794`
  - `since = 1770942114794`로 설정
  - Lichess API는 `since` 이상(>=)의 게임을 반환하므로 같은 게임이 다시 조회됨

### 로그에서 확인된 현상
```
[Batch-Trigger] [SINCE] 마지막 게임: since=1594134084203 (177005555816ms 이후 게임 조회)
```
→ 가장 최신 게임을 다시 조회하게 되는 문제

---

## 해결 방안

### 1. JPA 쿼리 수정
**파일**: `UserDailyStreakJpaRepository.java`

**변경 전**:
```java
@Query("""
  SELECT uds.lastGameAt
  FROM UserDailyStreakEntity uds
  WHERE uds.userId = :userId
  ORDER BY uds.lastGameAt ASC
  LIMIT 1
""")
Optional<Long> findLastGameAtByUserId(@Param("userId") Long userId);
```

**변경 후**:
```java
@Query("""
  SELECT uds.lastGameAt
  FROM UserDailyStreakEntity uds
  WHERE uds.userId = :userId
  ORDER BY uds.lastGameAt DESC
  LIMIT 1
""")
Optional<Long> findLastGameAtByUserId(@Param("userId") Long userId);
```

**이유**: 
- `DESC`(내림차순)로 정렬하여 **가장 최신 게임의 `lastGameAt`** 값을 조회
- 이전에는 `ASC`로 첫 게임을 조회하고 있었음

---

### 2. since 값에 +1ms 추가
**파일**: `UserBatchServiceImpl.java`

**변경 내용**:
```java
if (lastGameAt != null && lastGameAt > 0) {
    // 마지막 게임의 lastMoveAt + 1ms를 since로 설정하여 중복 조회 방지
    since = lastGameAt + 1;
    log.info("[Batch-Trigger] [SINCE] 마지막 게임: lastGameAt={}, since={} (+1ms 적용 | {}ms 이후 게임 조회)",
        lastGameAt, since, System.currentTimeMillis() - lastGameAt);
}
```

**이유**:
- Lichess API는 `since >= 조건`으로 게임을 조회
- `lastGameAt + 1`을 설정하면 그 이후의 새로운 게임만 조회됨
- 1ms 간격으로 충분한 정확도 제공 (Lichess API의 시간 단위가 ms)

---

### 3. 상세 로깅 추가
**파일**: `LichessApiTaskHandler.java`

**배치 완료 후 로깅**:
```java
Long lastGameAt = userDailyStreakRepository.findLastGameAtByUserId(task.userId());
if (lastGameAt != null && lastGameAt > 0) {
    log.info("[Worker-GAMES] [NEXT-SINCE] 다음 배치 조회 기준: lastGameAt={}, nextSince={} ({}시간 전)",
        lastGameAt, lastGameAt + 1, (System.currentTimeMillis() - lastGameAt) / (1000.0 * 60 * 60));
}
```

**효과**:
- 각 배치 완료 후 다음 `since` 값을 명확히 표시
- 증분 조회의 연쇄 흐름을 시각적으로 확인 가능
- 디버깅 및 모니터링 편의성 향상

---

## UUID 배치 ID와의 조합

이전에 적용된 UUID 기반 배치 ID와 이번 `since` 값 개선이 함께 동작:

1. **UUID 배치 ID** 
   - 용도: Spring Batch의 `JobInstanceAlreadyCompleteException` 방지
   - 효과: 같은 파라미터로 배치를 반복 실행 가능

2. **`since = lastGameAt + 1`**
   - 용도: 실제 게임 조회 범위 제어
   - 효과: 중복 조회 방지 (증분 조회의 핵심)

---

## 예상 효과

### Before (문제 상황)
```
배치 1 (첫 실행):     [게임 1~81개 조회] ✓
배치 2 (증분):       since=1594134084203 → [게임 80~81개 중복 + 새 게임 조회] ✗
배치 3 (증분):       since=1594134084203 → [게임 80~81개 중복 + 새 게임 조회] ✗
```

### After (개선 후)
```
배치 1 (첫 실행):     [게임 1~81개 조회] ✓
배치 2 (증분):       since=1594134084204 (+1) → [새 게임만 조회] ✓
배치 3 (증분):       since=[최신 게임의 lastGameAt + 1] → [새 게임만 조회] ✓
```

---

## 배포 전 검증 사항

1. ✅ 데이터베이스에 마이그레이션 불필요 (쿼리만 수정)
2. ✅ 하위 호환성 유지 (API 변경 없음)
3. ✅ 로그 출력 증가 (모니터링 개선)
4. ✅ 성능 영향 무시할 수 있는 수준 (+1ms는 시스템 부하 없음)

---

## 배포 순서

1. `UserDailyStreakJpaRepository.java` 쿼리 수정
2. `UserBatchServiceImpl.java` `since` 계산 로직 수정
3. `LichessApiTaskHandler.java` 로깅 추가
4. 전체 프로젝트 빌드 및 테스트
5. 스테이징 환경에서 게임 조회 배치 1회 실행
6. 로그 확인 후 프로덕션 배포

---

## 모니터링 포인트

배포 후 다음 로그 메시지를 확인하여 정상 동작 여부 판단:

```
[Batch-Trigger] [SINCE] 마지막 게임: lastGameAt=XXXX, since=YYYY (+1ms 적용 | ZZZ시간 전)
[Worker-GAMES] [NEXT-SINCE] 다음 배치 조회 기준: lastGameAt=XXXX, nextSince=YYYY (ZZZ시간 전)
```

→ `since` 값이 정상적으로 증가하면 배치가 올바르게 동작하는 것

