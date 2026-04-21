# 설계: OAuth 토큰 없는 유저 수집 지원

## 배경

운영 DB에서 유저를 직접 INSERT할 때 Redis에 OAuth 토큰이 없다.
현재 코드는 토큰이 없으면 즉시 수집을 중단(`expireToken`)하므로 해당 유저들의 게임이 수집되지 않는다.

Lichess `/api/games/user/{username}` 과 Chess.com 게임 아카이브 API는 모두 **인증 없이 접근 가능한 공개 API**이므로,
토큰 없이도 게임 수집을 진행하도록 설계를 변경한다.

---

## 플랫폼별 현황

### Lichess

| 위치 | 현재 동작 | 변경 필요 |
|------|----------|----------|
| `process()` | 토큰 null → `expireToken()` 후 return | ✅ 제거 |
| `processFullSync()` 루프 내 | 매 청크마다 토큰 재확인 → null이면 중단 | ✅ 제거 |
| `processIncremental()` 루프 내 | 동일 | ✅ 제거 |
| `LichessApi.getGames()` | `@RequestHeader("Authorization")` 필수 | ✅ optional로 변경 |

### Chess.com

| 위치 | 현재 동작 | 변경 필요 |
|------|----------|----------|
| `process()` | 토큰 null 허용 → sequential fallback | ✅ 없음 |
| `fetchAndSave()` | `authHeader = token != null ? "Bearer " + token : null` | ✅ 없음 |
| 병렬/순차 분기 | 토큰 있으면 parallel, 없으면 sequential | ✅ 없음 (이미 처리됨) |

Chess.com은 **이미 토큰 없는 수집을 지원**하고 있다. 변경 불필요.

---

## Rate Limit 검토

| 플랫폼 | 인증 없을 때 한도 | 현재 RateLimiter | 여유 |
|--------|-----------------|-----------------|------|
| Lichess | 20 req/min | 0.05 req/s = 3 req/min | ✅ 안전 |
| Chess.com | 명시 없음 (실운용 1~2 req/s 권장) | 0.5 req/s (sequential) | ✅ 안전 |

RateLimiter 값 변경 불필요.

---

## Lichess 변경 설계

### 1) `LichessApi.java`

Authorization 헤더를 선택적으로 변경.
null 전달 시 Spring HTTP Interface가 헤더 자체를 생략 → 비인증 요청으로 처리됨.

```java
// Before
@RequestHeader("Authorization") String authHeader

// After
@RequestHeader(value = "Authorization", required = false) String authHeader
```

### 2) `LichessGameSyncWorker.java`

**핵심 변경**: 토큰 null = 중단이 아닌 비인증 진행.

```
private String buildAuthHeader(String token) {
    return token != null ? "Bearer " + token : null;
}
```

이 헬퍼를 모든 `lichessApi.getGames()` 호출부에 적용.

#### `process()` 변경 전/후

```
// Before
String accessToken = tokenStore.getLichessAccessToken(userId);
if (accessToken == null) {
    job.expireToken();
    syncJobRepository.save(job);
    return;
}

// After
String accessToken = tokenStore.getLichessAccessToken(userId);
if (accessToken == null) {
    log.info("[LichessWorker] 토큰 없음 — 공개 API로 수집 진행 userId={}", userId);
}
```

#### `processFullSync()` 루프 내 변경 전/후

```
// Before (매 청크마다 토큰 재확인 후 abort)
String token = tokenStore.getLichessAccessToken(userId);
if (token == null) { job.expireToken(); syncJobRepository.save(job); return; }
lichessApi.getGames("Bearer " + token, ...)

// After (최초 process()에서 가져온 토큰 재사용, null이면 헤더 생략)
lichessApi.getGames(buildAuthHeader(accessToken), ...)
```

루프 내 토큰 재조회를 제거하고, `process()`에서 받은 `accessToken`을 파라미터로 전달.

#### `processIncremental()` 동일 패턴 적용.

---

## 파라미터 전달 흐름 변경

현재 `processFullSync(job, userId, username)` 시그니처에 `accessToken`을 추가로 전달.

```
process(job)
  accessToken = tokenStore.getLichessAccessToken(userId)  // null 허용
  ├─ processFullSync(job, userId, username, accessToken)
  └─ processIncremental(job, userId, username, latestPlayedAt, accessToken)
```

루프 내에서 매번 Redis를 다시 조회하던 불필요한 호출도 제거되는 부수 효과.

---

## 나중에 유저가 OAuth 로그인 시

토큰이 Redis에 저장되면 다음 30분 스케줄러 실행부터 자동으로 인증 모드로 전환됨.
별도 처리 불필요.

```
첫 수집 (토큰 없음)  → buildAuthHeader(null) → null 헤더 → 공개 API (3 req/min)
OAuth 로그인 후      → buildAuthHeader(token) → Bearer xxx → 인증 API (더 높은 한도)
```

---

## DB INSERT 주의사항

스케줄러는 `lichessUserRepository.findAll()` → `lichess_users` 테이블을 조회.

```sql
-- 틀림
INSERT INTO users (...) VALUES (...)

-- 올바름
INSERT INTO lichess_users (...) VALUES (...)
```

`deleted_at` 컬럼은 INSERT에 포함하지 않아도 됨 (null 허용).

---

## 변경 파일 요약

| 파일 | 변경 내용 |
|------|----------|
| `LichessApi.java` | `Authorization` 헤더 `required = false` |
| `LichessGameSyncWorker.java` | 토큰 null abort 제거, `buildAuthHeader()` 헬퍼 추가, 메서드 시그니처에 `accessToken` 추가 |
| `ChessComGameSyncWorker.java` | 변경 없음 |
