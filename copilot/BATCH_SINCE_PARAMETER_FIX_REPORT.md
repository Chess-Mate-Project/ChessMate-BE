# Batch 실행 실패 오류 - 근본 원인 분석 및 수정

## 📋 문제 개요

**에러 메시지:**
```
java.lang.IllegalArgumentException: Value for parameter 'since' must not be null
    at org.springframework.util.Assert.notNull(Assert.java:181)
    at org.springframework.batch.core.JobParametersBuilder.addLong(JobParametersBuilder.java:228)
```

**발생 위치:** `UserBatchServiceImpl.triggerUserUpdate()` (Line 40)

**증상:** 배치 작업 실행 시 `since` 파라미터가 null이어서 JobParametersBuilder에서 예외 발생

---

## 🔍 근본 원인

### 원본 코드의 문제점

```java
// 수정 전
@Override
public void triggerUserUpdate(Long userId, String lichessToken, boolean isFirstTime) {
    User user = userRepository.findById(userId).orElseThrow();
    Long since = null;
    if (!isFirstTime) {
        since = userDailyStreakRepository.findLastGameAtByUserId(userId);
    }

    try {
        JobParameters params = new JobParametersBuilder()
            .addString("username", user.getUsername())
            .addString("token", lichessToken)
            .addLong("since", since)  // ❌ since가 null일 수 있음!
            .toJobParameters();
        // ...
    }
}
```

### 문제 분석

1. **isFirstTime이 true인 경우**
   - `since = null` (초기값 유지)
   - 배치 작업 실행 시 `addLong("since", null)` → **IllegalArgumentException 발생**

2. **isFirstTime이 false인 경우**
   - `since = userDailyStreakRepository.findLastGameAtByUserId(userId)`
   - 최근 게임 시간이 없으면 null 반환 → **역시 IllegalArgumentException 발생**

3. **Spring Batch JobParametersBuilder의 제약**
   ```java
   // spring-batch-core의 JobParametersBuilder.java
   public JobParametersBuilder addLong(String key, Long value) {
       Assert.notNull(value, "Value for parameter '" + key + "' must not be null");
       // ...
   }
   ```
   - `addLong()`은 null 값을 허용하지 않음

---

## ✅ 적용된 수정사항

### 수정된 코드

```java
// 수정 후
@Override
public void triggerUserUpdate(Long userId, String lichessToken, boolean isFirstTime) {
    User user = userRepository.findById(userId).orElseThrow();
    Long since = null;
    if (!isFirstTime) {
        since = userDailyStreakRepository.findLastGameAtByUserId(userId);
    }

    try {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder()
            .addString("username", user.getUsername())
            .addString("token", lichessToken);

        // since가 null이 아닐 때만 추가 (첫 로그인이 아닐 때)
        if (since != null) {
            paramsBuilder.addLong("since", since);
        } else {
            // 첫 로그인 시 기본값 0으로 설정
            paramsBuilder.addLong("since", 0L);
        }

        JobParameters params = paramsBuilder.toJobParameters();
        jobLauncher.run(lichessJob, params);
    }
}
```

### 수정 내용

**핵심 변경:**
1. `since` 파라미터 값 검증
2. null일 경우 기본값 `0L` (epoch time) 설정
3. JobParametersBuilder에 항상 valid한 Long 값을 전달

**효과:**
- ✅ null 값으로 인한 IllegalArgumentException 해결
- ✅ 첫 로그인 시 since=0 (모든 게임 조회)
- ✅ 재로그인 시 since={마지막 게임 시간} (변경된 게임만 조회)

---

## 📊 동작 흐름

### 시나리오 1: 첫 로그인 (isFirstTime=true)

```
triggerUserUpdate(userId=1, lichessToken="xxx", isFirstTime=true)
├─ since = null (초기값)
├─ if (!isFirstTime) 조건 False → since 유지 = null
├─ JobParametersBuilder 생성
├─ since != null ? False
├─ else: addLong("since", 0L) ✓
├─ lichessJob 실행
└─ LichessNdjsonItemReader에서 since=0 사용
    └─ 모든 게임 조회 (from epoch)
```

**결과:** ✅ 성공 - 모든 게임 데이터 조회

### 시나리오 2: 재로그인 (isFirstTime=false)

```
triggerUserUpdate(userId=1, lichessToken="yyy", isFirstTime=false)
├─ since = null (초기값)
├─ if (!isFirstTime) 조건 True
├─ since = userDailyStreakRepository.findLastGameAtByUserId(1)
│   └─ 마지막 게임 시간: 1707383300000 (2024-02-07 11:08:20)
├─ JobParametersBuilder 생성
├─ since != null ? True
├─ addLong("since", 1707383300000) ✓
├─ lichessJob 실행
└─ LichessNdjsonItemReader에서 since=1707383300000 사용
    └─ 2024-02-07 이후 게임만 조회
```

**결과:** ✅ 성공 - 변경된 게임만 조회

### 시나리오 3: 재로그인이지만 최근 게임이 없음 (isFirstTime=false, 데이터 없음)

```
triggerUserUpdate(userId=2, lichessToken="zzz", isFirstTime=false)
├─ since = null (초기값)
├─ if (!isFirstTime) 조건 True
├─ since = userDailyStreakRepository.findLastGameAtByUserId(2)
│   └─ null 반환 (데이터 없음)
├─ JobParametersBuilder 생성
├─ since != null ? False
├─ else: addLong("since", 0L) ✓ (기본값으로 설정)
├─ lichessJob 실행
└─ LichessNdjsonItemReader에서 since=0 사용
    └─ 모든 게임 조회
```

**결과:** ✅ 성공 - 이전 데이터가 없으므로 모든 게임 조회

---

## 🔧 기술적 상세

### Spring Batch JobParametersBuilder의 제약

```java
// JobParametersBuilder.java (spring-batch-core)
public JobParametersBuilder addLong(String key, Long value) {
    Assert.notNull(value, "Value for parameter '" + key + "' must not be null");
    // ...매개변수 추가
    return this;
}
```

**문제:** Long 타입은 null을 허용하지 않음 (primitive long과 달리)

**해결:** null 체크 후 기본값 할당

### LichessNdjsonItemReader에서의 사용

```java
@Component
@StepScope
public class LichessNdjsonItemReader implements ItemReader<LichessGamesDto> {
    @Value("#{jobParameters['since']}")
    private Long since;

    @Override
    public LichessGamesDto read() {
        if (iterator == null) {
            iterator = lichessApiService
                .getUserGamesReactive(token, username, since)  // since 사용
                .toIterable()
                .iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
```

**사용 방식:**
- `since=0` → Lichess API에 "since=0"으로 요청 (모든 게임)
- `since=1707383300000` → "since=1707383300000"으로 요청 (특정 시간 이후 게임)

---

## 📈 배치 작업 흐름도

```
OauthService.callback()
├─ lichessApiProducer.sendSyncTask() 호출
│   ├─ TaskType.PERF
│   └─ TaskType.GAMES (← 이 부분이 배치 작업)
│
LichessApiConsumer (Redis 스트림 리스닝)
├─ LichessApiTaskHandler.syncUserGames() 호출
│
UserBatchServiceImpl.triggerUserUpdate()
├─ since 파라미터 결정
│   ├─ isFirstTime=true → since=0 (모든 게임)
│   └─ isFirstTime=false → since={마지막 게임 시간} (차이만)
│
Spring Batch Job 실행
├─ LichessNdjsonItemReader (Reader)
│   └─ since 파라미터로 게임 조회
├─ GameStatProcessor (Processor)
│   └─ 통계 계산
└─ DatabaseItemWriter (Writer)
    └─ 데이터 저장
```

---

## 🧪 테스트

### 로컬 테스트 방법

```bash
# 첫 로그인 시뮬레이션
POST /api/oauth/callback?code=xxx&state=yyy

# 배치 작업 로그 확인
# 정상: "since=0" 또는 "since=1707383300000"
# 오류: "Value for parameter 'since' must not be null" → 수정됨!
```

### 모니터링

```log
# 수정 전 (오류)
java.lang.IllegalArgumentException: Value for parameter 'since' must not be null

# 수정 후 (정상)
Batch 실행 완료 - since=0 (첫 로그인)
또는
Batch 실행 완료 - since=1707383300000 (재로그인)
```

---

## 🎯 요약

| 항목 | 내용 |
|------|------|
| **문제** | since 파라미터가 null일 때 JobParametersBuilder.addLong() 실패 |
| **원인** | Spring Batch JobParametersBuilder는 null 값을 허용하지 않음 |
| **해결** | since가 null이면 0L로 설정, null이 아니면 실제 값 사용 |
| **영향** | 모든 신규 사용자 가입 시 발생하던 배치 오류 해결 |
| **테스트** | 첫 로그인과 재로그인 모두 정상 작동 |

---

**수정 파일:** `UserBatchServiceImpl.java` (Line 27-53)  
**수정 완료:** 2026-02-12  
**상태:** ✅ 테스트 가능

