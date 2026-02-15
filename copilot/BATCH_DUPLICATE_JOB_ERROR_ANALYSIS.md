# Spring Batch Job Instance 중복 조회 에러 분석 보고서

**작성일**: 2026-02-15  
**프로젝트**: ChessMate  
**담당자**: Backend Development Team

---

## 1. 문제 현상

### 에러 메시지
```
A job instance already exists and is complete for identifying parameters=
{'username':'mg0922', 'token':'lio_eoHiBL22e2DGED6KlGkTIBc2mK0uuwt2', 'since':'1594134084203'}
```

### 관찰 내용
- Worker에서 동일한 게임을 반복 조회 (스트릭 데이터 중복 저장)
- 같은 `since` 파라미터로 배치 재실행 시도
- `JobInstanceAlreadyCompleteException` 발생

---

## 2. 근본 원인 분석

### Spring Batch의 작동 원리

Spring Batch는 **Job Instance**를 `JobParameters`의 조합으로 식별합니다:

```
Job Instance ID = Job Name + Parameters (username, token, since)
```

**Spring Batch의 설계 원칙:**
- 동일한 `JobParameters`를 가진 Job은 **한 번만 완료 가능**
- 재실행 시도 시 `JobInstanceAlreadyCompleteException` 발생
- 이는 **데이터 무결성 보호**를 위한 의도된 동작

### 현재 코드의 문제점

```java
JobParametersBuilder paramsBuilder = new JobParametersBuilder()
    .addString("username", user.getUsername())        // ❌ 문제: 동일한 값
    .addString("token", lichessToken)                  // ❌ 문제: 동일한 값
    .addLong("since", since);                          // ❌ 문제: 동일한 값
```

**동일한 파라미터로 재실행되는 상황:**

| 실행 횟수 | username | token | since | 결과 |
|---------|----------|-------|-------|------|
| 1차 | mg0922 | lio_eoHiBL... | 1594134084203 | ✅ 성공 |
| 2차 | mg0922 | lio_eoHiBL... | 1594134084203 | ❌ JobInstanceAlreadyCompleteException |
| 3차 | mg0922 | lio_eoHiBL... | 1594134084203 | ❌ JobInstanceAlreadyCompleteException |

### 중복 조회가 발생하는 이유

1. **Redis Task 재시도**: 첫 번째 실행 후 에러 발생
2. **재시도 로직**: Consumer의 backoff 메커니즘으로 재시도
3. **동일 파라미터**: 같은 `since` 값으로 재실행 시도
4. **Batch 거부**: Job Instance 이미 존재하여 거부
5. **루프**: Redis 큐에 남아있는 Task 반복 처리

---

## 3. 해결 방안

### ✅ 권장 방안: Job Run ID 추가

`JobParameters`에 **고유한 실행 ID** 추가:

```java
JobParametersBuilder paramsBuilder = new JobParametersBuilder()
    .addString("username", user.getUsername())
    .addString("token", lichessToken)
    .addLong("since", since)
    .addLong("run_id", System.currentTimeMillis())  // ✨ 고유 ID 추가
    .toJobParameters();
```

**장점:**
- 매번 다른 `JobParameters` 생성
- Job 재실행 가능
- Spring Batch의 설계 원칙 준수
- 간단하고 효과적

### 📋 구현 코드

```java
@Override
public void triggerUserUpdate(Long userId, String lichessToken, boolean isFirstTime) {
    // ... 기존 로직 ...
    
    try {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder()
            .addString("username", user.getUsername())
            .addString("token", lichessToken);

        if (since != null) {
            paramsBuilder.addLong("since", since);
        } else {
            paramsBuilder.addLong("since", 0L);
        }
        
        // ✨ 고유한 실행 ID 추가 (타임스탐프 + 나노초)
        paramsBuilder.addLong("runId", System.nanoTime());
        
        JobParameters params = paramsBuilder.toJobParameters();
        jobLauncher.run(lichessJob, params);
        
    } catch (JobInstanceAlreadyCompleteException e) {
        log.error("[Batch-Trigger] Job Instance 이미 존재 - {}ms 이내 재실행 시도 불가", 
                  "동일 파라미터");
    }
}
```

---

## 4. 상세 설명

### Spring Batch Job Instance의 동작

**Job Instance 식별 과정:**

```
Identifying Parameters = {username, token, since}
                        ↓
                  Hash 함수로 변환
                        ↓
            Job Instance 고유 ID 생성
                        ↓
          이전 실행 기록 조회 (BATCH_JOB_INSTANCE 테이블)
                        ↓
         이미 존재 + COMPLETED → JobInstanceAlreadyCompleteException
```

**Job Instance 상태:**

```sql
-- BATCH_JOB_INSTANCE 테이블 확인
SELECT * FROM BATCH_JOB_INSTANCE 
WHERE JOB_NAME = 'lichessJob'
AND JOB_KEY = HASH(username + token + since);

-- 예상 결과:
-- JOB_INSTANCE_ID | JOB_NAME | JOB_KEY | VERSION | STATUS
-- 1               | lichessJob | abc... | 1     | COMPLETED
```

---

## 5. 예상 효과

### 수정 전
```
[16:14:00] 배치 작업 실행: username=mg0922, since=1594134084203
[16:14:00] ❌ JobInstanceAlreadyCompleteException
[16:14:00] 재시도 (backoff 1000ms)
[16:14:01] ❌ JobInstanceAlreadyCompleteException (동일)
[16:14:02] ❌ JobInstanceAlreadyCompleteException (동일)
```

### 수정 후
```
[16:14:00] 배치 작업 실행: username=mg0922, since=1594134084203, runId=1234567890
[16:14:01] ✅ 배치 작업 완료
[16:14:02] 재시도 (backoff 1000ms)
[16:14:03] 배치 작업 실행: username=mg0922, since=1594134084203, runId=1234567891
[16:14:04] ✅ 배치 작업 완료
```

---

## 6. 추가 개선사항

### 1. Job 중복 실행 방지 (선택사항)

만약 **동일한 데이터**로 Job 실행을 방지하려면:

```java
// JobLauncher 설정에서
JobLauncher jobLauncher = new TaskExecutorJobLauncher();
jobLauncher.setJobRepository(jobRepository);
jobLauncher.setTaskExecutor(simpleAsyncTaskExecutor());
jobLauncher.setRethrowExceptionOnStartupFailure(false);  // 예외 무시 옵션
```

### 2. Redis Task 중복 제거

```java
// LichessApiTaskHandler에서 이미 실행 중인 Task 확인
if (redisService.hasKey("batch:running:" + userId)) {
    log.info("배치 작업 이미 진행 중: userId={}", userId);
    return;
}

// 배치 완료 후 제거
redisService.delete("batch:running:" + userId);
```

### 3. 타임스탐프 기반 중복 감지

```java
// 마지막 배치 실행 시간 저장
long lastBatchTime = redisService.getLong("batch:last:" + userId);
if (System.currentTimeMillis() - lastBatchTime < 60_000) {
    log.warn("배치 작업 재실행 쿨타임 중: userId={}", userId);
    return;
}
```

---

## 7. 결론

| 항목 | 내용 |
|------|------|
| **근본 원인** | 동일한 JobParameters로 Job 재실행 시도 |
| **Spring Batch 동작** | Job Instance 중복 방지 (의도된 설계) |
| **해결 방안** | JobParameters에 고유 ID 추가 |
| **구현 난도** | ⭐⭐ (매우 간단) |
| **영향 범위** | UserBatchServiceImpl.triggerUserUpdate() 메서드 |
| **예상 효과** | ✅ Job 재실행 가능, ✅ 중복 조회 해결 |

---

## 8. 검증 방법

**변경 후 확인:**

```
1. Worker 로그 확인
   - "배치 작업 실행 완료" 메시지 출력 확인
   - JobInstanceAlreadyCompleteException 미발생
   
2. 데이터베이스 확인
   - BATCH_JOB_INSTANCE 테이블에 새로운 runId로 레코드 생성
   - 각 runId별 하나씩만 COMPLETED 상태
   
3. 스트릭 데이터 확인
   - user_daily_streak 테이블에서 중복 없음
   - date + user_id 조합별 정확히 1개 레코드
```


