# Force-Refresh 엔드포인트 캐시 미삭제 문제 보고서

## 문제 상황

사용자가 `/api/stat/force-refresh` 엔드포인트를 호출했을 때, **캐시가 재갱신되지 않는 문제** 발생

현재 상황:
- API를 호출하면 GAMES/ACCOUNT/PERF 작업이 Worker 큐에 전송됨
- Worker에서 DB 데이터는 업데이트됨
- **하지만 Redis 캐시는 삭제되지 않음**
- 따라서 프론트에서 요청하면 여전히 오래된 캐시 데이터 반환

## 문제의 근본 원인

### 1. UpdateDataService에서 캐시 삭제 없음

**파일**: `chessmate-worker/src/main/java/com/chessmate/worker/batch/service/UpdateDataService.java`

```java
public void updateUserGameData(User user, String batchId) {
    // ... 작업 생성 코드 ...
    
    lichessApiRedisService.pushTask(gamestask);
    lichessApiRedisService.pushTask(accounttask);
    lichessApiRedisService.pushTask(perftask);
    
    // ❌ 캐시 삭제 로직 없음
}
```

### 2. CacheService에서 제공하는 삭제 메서드들

CacheService에는 이미 캐시 삭제 메서드들이 준비되어 있음:

| 메서드 | 용도 |
|--------|------|
| `deletePlayTime(String lichessId)` | 플레이 시간 캐시 삭제 |
| `deletePerfs(String lichessId)` | 게임 통계 (Perfs) 캐시 삭제 |
| `deleteUserCount(String lichessId)` | 플레이 횟수 캐시 삭제 |
| `deleteAllRankings()` | 모든 랭킹 캐시 삭제 (4개 게임 타입) |
| `deleteCache(String key)` | 범용 캐시 삭제 |
| `deleteLichessToken(Long id)` | Lichess OAuth 토큰 삭제 |

### 3. StatService에서 force-refresh 호출 순서

**파일**: `chessmate-api/src/main/java/com/chessmate/api/stat/service/StatService.java`

```java
public void forceUpdateUserData(User user) {
    lichessApiProducer.sendSyncTask(
        user, 
        user.getUsername(), 
        cacheService.getLichessToken(user.getId()), 
        TaskType.FORCE_UPDATE, 
        false
    );
    // ❌ 캐시 삭제 로직 없음
}
```

## 현재 데이터 흐름 분석

### FORCE_UPDATE 작업 처리

**파일**: `chessmate-worker/src/main/java/com/chessmate/worker/batch/redis/LichessApiTaskHandler.java`

```java
if (task.type() == TaskType.FORCE_UPDATE) {
    log.info("[Worker-Handler] FORCE_UPDATE 작업 처리 - userId={}", task.userId());
    updateDataService.updateUserGameData(
        userRepository.findById(task.userId()).orElseThrow(),
        null
    );
    log.info("[Worker-Handler] FORCE_UPDATE 작업 완료");
    return;
}
```

**흐름**:
1. StatService.forceUpdateUserData() 호출
2. LichessApiProducer.sendSyncTask() → Redis 큐에 FORCE_UPDATE 작업 전송
3. Worker의 LichessApiTaskHandler.handle() → FORCE_UPDATE 처리
4. UpdateDataService.updateUserGameData() → GAMES/ACCOUNT/PERF 작업 생성 및 큐 전송
5. Worker에서 각 작업 처리:
   - ACCOUNT 작업: DB의 User 엔티티 업데이트 ✓
   - PERF 작업: DB의 UserPerf 엔티티 업데이트 ✓
   - GAMES 작업: DB의 DailyStreak 엔티티 업데이트 ✓
6. **❌ 캐시 삭제 단계 없음**

## 영향받는 캐시들

force-refresh 후에 갱신되어야 하는 캐시:

| 캐시 | 저장 위치 | 현재 상태 |
|-----|---------|--------|
| Perfs (RAPID/BLITZ/BULLET/CLASSICAL) | Redis | ❌ 삭제 안 됨 |
| PlayTime | Redis | ❌ 삭제 안 됨 |
| UserCount | Redis | ❌ 삭제 안 됨 |
| 모든 Ranking (4개 게임 타입) | Redis | ❌ 삭제 안 됨 |
| 연간 스트릭 (YearStreak) | Redis | ❌ 삭제 안 됨 |
| 사용자 프로필 (UserProfile) | Redis | ❌ 삭제 안 됨 |

## 해결 방안

### Option 1: UpdateDataService에서 캐시 삭제 (권장)

UpdateDataService의 `updateUserGameData()` 메서드에서 작업 전송 후 캐시 삭제:

```java
public void updateUserGameData(User user, String batchId) {
    // ... 기존 작업 생성 및 전송 코드 ...
    
    lichessApiRedisService.pushTask(gamestask);
    lichessApiRedisService.pushTask(accounttask);
    lichessApiRedisService.pushTask(perftask);
    
    // ✅ 캐시 삭제 추가
    String lichessId = user.getLichessId();
    cacheService.deletePlayTime(lichessId);
    cacheService.deletePerfs(lichessId);
    cacheService.deleteUserCount(lichessId);
    cacheService.deleteAllRankings();
    
    log.info("[UpdateDataService] 캐시 삭제 완료 - userId={}, lichessId={}", 
             user.getId(), lichessId);
}
```

### Option 2: StatService에서 직접 캐시 삭제

StatService의 `forceUpdateUserData()` 메서드에서 캐시 삭제:

```java
public void forceUpdateUserData(User user) {
    // 1. 캐시 먼저 삭제
    String lichessId = user.getLichessId();
    cacheService.deletePlayTime(lichessId);
    cacheService.deletePerfs(lichessId);
    cacheService.deleteUserCount(lichessId);
    cacheService.deleteAllRankings();
    
    log.info("[StatService] 캐시 삭제 완료 - userId={}, lichessId={}", 
             user.getId(), lichessId);
    
    // 2. 데이터 갱신 작업 전송
    lichessApiProducer.sendSyncTask(
        user, 
        user.getUsername(), 
        cacheService.getLichessToken(user.getId()), 
        TaskType.FORCE_UPDATE, 
        false
    );
}
```

## 권장 구현 전략

**Option 1이 더 적절한 이유**:

1. **책임 분리**: 실제 캐시 삭제 로직을 Worker의 UpdateDataService에 배치
2. **일관성**: 데이터 업데이트와 캐시 무효화가 같은 위치에서 처리
3. **트랜잭션**: Worker 내에서 DB 업데이트와 캐시 삭제를 연관시킬 수 있음
4. **재사용성**: 향후 다른 경로에서도 UpdateDataService 호출 시 캐시 자동 삭제

## 구현 시 고려사항

### 1. 삭제 순서

```java
// 먼저 상세 캐시 삭제
cacheService.deletePlayTime(lichessId);      // 플레이 시간
cacheService.deletePerfs(lichessId);         // 게임 통계
cacheService.deleteUserCount(lichessId);     // 플레이 횟수

// 마지막에 랭킹 삭제 (모든 게임 타입 영향)
cacheService.deleteAllRankings();
```

### 2. 로깅

```java
log.info("[UpdateDataService] === 캐시 삭제 시작 === userId={}, lichessId={}", 
         user.getId(), lichessId);
log.info("[UpdateDataService] 삭제된 캐시: PlayTime, Perfs, UserCount, AllRankings");
log.info("[UpdateDataService] === 캐시 삭제 완료 ===");
```

### 3. 예외 처리

Redis 연결 실패 시에도 DB 업데이트가 진행되어야 함:

```java
try {
    // 캐시 삭제 시도
    cacheService.deletePlayTime(lichessId);
    cacheService.deletePerfs(lichessId);
    cacheService.deleteUserCount(lichessId);
    cacheService.deleteAllRankings();
} catch (Exception e) {
    log.warn("[UpdateDataService] 캐시 삭제 실패 - userId={}, error={}", 
             user.getId(), e.getMessage());
    // 계속 진행 (Redis 실패가 DB 업데이트를 막지 않음)
}
```

## 추가 개선 사항

### 1. User 엔티티에 LichessId 필드 확인

UpdateDataService에서 `user.getLichessId()` 사용 예정이므로 필드 존재 확인:

```java
// User 엔티티에 lichessId 필드가 있는지 확인
private String lichessId;  // 있어야 함
```

### 2. 프로필 캐시도 함께 삭제 고려

사용자 프로필 캐시도 함께 삭제해야 할 수 있음:

```java
// 사용자 프로필 캐시 삭제 (있다면)
cacheService.deleteCache(buildUserProfileCacheKey(user.getId()));
```

## 예상 효과

구현 후 force-refresh 호출 시:

1. **즉시 효과**: Redis 캐시 삭제
2. **다음 요청**: Cache-Aside 패턴에 따라 DB에서 최신 데이터 조회
3. **프론트 갱신**: 새로운 데이터로 UI 업데이트

## 테스트 시나리오

```
1. force-refresh 호출 직전: Redis에 기존 캐시 존재
2. force-refresh 호출
3. Redis 캐시 확인 → 삭제됨
4. API 요청 → 새 데이터 반환
5. Redis 캐시 확인 → 새 데이터로 저장됨
```

---

**작성일**: 2026-02-15  
**상태**: 문제 확인 완료, 해결책 제시 완료  
**우선순위**: 높음 (사용자 경험에 직결)

