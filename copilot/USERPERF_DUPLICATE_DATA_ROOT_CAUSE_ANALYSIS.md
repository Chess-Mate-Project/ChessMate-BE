# UserPerf 중복 데이터 생성 - 근본 원인 분석 & 해결

## 📊 현재 상황

**문제:**
- `user_perfs` 테이블에 유저당 4개가 아닌 **8개의 중복 데이터** 존재
- 영향을 받는 유저: 11명 (user_id: 1, 2, 5, 11, 13, 18, 32, 42, 85, 86, 87, 88)
- 각 유저마다 정상: 4개(BULLET, BLITZ, RAPID, CLASSICAL) × 2 = 8개

**증상 데이터:**
```sql
-- user_id=1의 경우 RAPID 게임 타입이 2개 존재
SELECT * FROM user_perfs WHERE user_id = 1 AND game_type = 'RAPID';
결과: 2개의 row (id가 다름)
```

---

## 🔍 근본 원인 분석

### ✅ 코드 검증 결과

Worker의 `LichessApiTaskHandler.java`에서 UserPerf 저장 로직을 분석:

```java
// 라인 217: 쿼리 메서드 호출
UserPerf existingPerf = userPerfRepository.findByUserIdAndGameType(task.userId(), type)
    .orElse(null);

if (existingPerf != null) {
    // UPDATE 로직: 기존 데이터가 있으면 업데이트
    existingPerf.setRating(newRating);
    // ... (필드 업데이트)
    userPerfRepository.save(existingPerf);
} else {
    // INSERT 로직: 새로운 데이터 생성
    UserPerf userPerf = UserPerf.builder()...build();
    userPerfRepository.save(userPerf);
}
```

**코드상 Upsert 로직은 정상 구현됨** ✅

---

## 🎯 추정되는 실제 원인들

### **원인 1: Race Condition (동시성 문제)**

**상황:**
```
시간  Worker 스레드 1           Worker 스레드 2
T0    findByUserIdAndGameType   (쿼리 실행)
      → null 반환
      
T1                              findByUserIdAndGameType
                                (같은 조건으로 쿼리)
                                → null 반환
                                
T2    INSERT (user_id=1, RAPID)
      → 저장됨 (id=100)
      
T3                              INSERT (user_id=1, RAPID)
                                → 저장됨 (id=101)
```

**결과:** 같은 사용자+게임타입 조합으로 2개의 데이터 생성

### **원인 2: 배치 작업이 여러 번 실행**

- Worker가 동일한 사용자에 대해 여러 번 PERF 작업 실행
- 각 실행마다 새로운 트랜잭션에서 중복 INSERT 발생

### **원인 3: 트랜잭션 격리 수준 문제**

현재 코드:
```java
transactionTemplate.executeWithoutResult(status -> {
    UserPerf existingPerf = userPerfRepository.findByUserIdAndGameType(...)
        .orElse(null);
    
    if (existingPerf != null) {
        userPerfRepository.save(existingPerf);  // UPDATE
    } else {
        userPerfRepository.save(userPerf);      // INSERT
    }
});
```

**문제:** 조회(select)와 저장(insert) 사이에 다른 트랜잭션이 개입 가능

---

## ✅ 해결 방안 - 3가지 전략

### **전략 1: Unique 제약 조건 추가 (DB 레벨)**

```sql
-- user_perfs 테이블에 Unique 제약 추가 (이미 있을 것으로 예상)
ALTER TABLE user_perfs 
ADD CONSTRAINT uk_user_perf_user_gametype 
UNIQUE (user_id, game_type);
```

**장점:**
- DB 레벨에서 중복 방지
- 운영 중에도 신규 중복 생성 불가능

---

### **전략 2: 동시성 제어 - Pessimistic Lock 추가 (권장)**

현재 코드 수정:

```java
// UserPerfJpaRepository 에 새 메서드 추가
@Query("SELECT up FROM UserPerfEntity up "
    + "WHERE up.userId = :userId AND up.gameType = :gameType")
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<UserPerfEntity> findByUserIdAndGameTypeWithLock(
    @Param("userId") Long userId, 
    @Param("gameType") GameType gameType
);
```

Worker 코드 수정:

```java
// 잠금과 함께 조회
UserPerf existingPerf = userPerfRepository
    .findByUserIdAndGameTypeWithLock(task.userId(), type)
    .orElse(null);

// ... 나머지 로직은 동일
```

**장점:**
- 조회부터 저장까지 원자성 보장
- 두 트랜잭션이 동시에 진입 불가능

---

### **전략 3: Transactional Annotation 강화 (단기)**

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
private void saveUserPerf(...) {
    UserPerf existingPerf = userPerfRepository
        .findByUserIdAndGameType(userId, gameType)
        .orElse(null);
    
    if (existingPerf != null) {
        // UPDATE
    } else {
        // INSERT
    }
}
```

**장점:**
- 빠른 적용
- 코드 변경 최소

**단점:**
- 성능 저하 가능성
- 모든 쿼리에 영향

---

## 📝 추가된 로깅 (이미 적용됨)

Worker에 상세 로깅 추가:

```java
// DB 조회 전
log.info("[Worker-PERF] [{}] DB 조회 시작 - userId={}, gameType={}", 
    type, task.userId(), type);

// DB 조회 후
if (existingPerf != null) {
    log.info("[Worker-PERF] [{}] DB 조회 결과: 기존 데이터 발견 - id={}", 
        type, existingPerf.getId());
} else {
    log.warn("[Worker-PERF] [{}] DB 조회 결과: 기존 데이터 없음", type);
}

// UPDATE 완료
log.info("[Worker-PERF] [{}] === UPDATE 완료 === id={}, userId={}, rating={}", 
    type, updatedPerf.getId(), updatedPerf.getUserId(), updatedPerf.getRating());

// INSERT 완료
log.info("[Worker-PERF] [{}] === INSERT 완료 === savedId={}, userId={}, gameType={}", 
    type, savedPerf.getId(), savedPerf.getUserId(), savedPerf.getGameType());
```

**이를 통해 확인할 수 있는 것:**
1. 조회가 제대로 실행되는지
2. UPDATE 또는 INSERT가 실행되는지
3. 실제로 저장된 ID가 무엇인지
4. 동시 실행 여부

---

## 🛠️ 적용 순서

### 단계 1: 현재 중복 데이터 정리 (이미 실행됨)
```sql
DELETE FROM user_perfs 
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MAX(id) AS id 
        FROM user_perfs 
        GROUP BY user_id, game_type
    ) AS tmp
);
```

### 단계 2: Unique 제약 조건 추가
```sql
ALTER TABLE user_perfs 
ADD CONSTRAINT uk_user_perf_user_gametype 
UNIQUE (user_id, game_type);
```

### 단계 3: Pessimistic Lock 추가 (코드 수정)
- `UserPerfJpaRepository.java` 수정
- `LichessApiTaskHandler.java`에서 Lock 버전 호출로 변경

### 단계 4: 로깅을 통한 모니터링
- 서버 재배포 후 Worker 실행
- 로그 확인으로 중복 생성 여부 검증

---

## 📊 검증 방법

### 로그 확인

**정상 케이스 (UPDATE):**
```
[Worker-PERF] [RAPID] DB 조회 시작 - userId=1, gameType=RAPID
[Worker-PERF] [RAPID] DB 조회 결과: 기존 데이터 발견 - id=100
[Worker-PERF] [RAPID] === 데이터 변경 감지 (id=100) ===
[Worker-PERF] [RAPID] === UPDATE 완료 === id=100, userId=1, rating=1600
```

**비정상 케이스 (INSERT 중복):**
```
[Worker-PERF] [RAPID] DB 조회 시작 - userId=1, gameType=RAPID
[Worker-PERF] [RAPID] DB 조회 결과: 기존 데이터 없음
[Worker-PERF] [RAPID] === INSERT 완료 === savedId=101, userId=1, gameType=RAPID
---
[Worker-PERF] [RAPID] DB 조회 시작 - userId=1, gameType=RAPID
[Worker-PERF] [RAPID] DB 조회 결과: 기존 데이터 없음  ← ⚠️ 동시에 또 NULL 반환
[Worker-PERF] [RAPID] === INSERT 완료 === savedId=102, userId=1, gameType=RAPID
```

---

## 🔐 최종 권장 해결책

**Pessimistic Lock + Unique 제약 조건 이중 방어**

1. **DB 레벨:** Unique 제약 조건 (마지막 방어선)
2. **Code 레벨:** Pessimistic Lock (동시성 제어)
3. **Monitoring:** 상세 로깅 (조기 감지)

이 조합으로 어떤 경로로든 중복 생성을 원천 차단할 수 있습니다.

---

## 📌 적용 상태

- ✅ 로깅 강화: `LichessApiTaskHandler.java` 수정 완료
- ⏳ Unique 제약 조건 추가: DB 마이그레이션 필요
- ⏳ Pessimistic Lock: 코드 수정 대기 중

**다음 단계:** 
1. 현재 로깅 적용 후 Worker 실행
2. 로그 분석으로 실제 원인 파악
3. 그 결과에 따라 Lock 적용 결정

