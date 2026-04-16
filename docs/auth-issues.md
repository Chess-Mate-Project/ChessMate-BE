# ChessMate 인증 구조 문제점 분석

> 분석 일자: 2026-04-16  
> 분석 범위: `chessmate-api/src/main/java/com/chessmate/api/global/auth/`

---

## 요약

| # | 심각도 | 위치 | 문제 | 상태 |
|---|--------|------|------|------|
| 1 | 🔴 심각 | `JwtUtil`, `JwtService`, `CookieName` | 쿠키 이름 체계 이중화로 로그아웃 미동작 | ✅ 수정완료 |
| 2 | 🔴 심각 | `PublicUserController`, `UserService` | 크로스 플랫폼 ID 충돌로 검색 결과 오제외 | ✅ 수정완료 |
| 3 | 🟠 중간 | `AuthController` | refresh 엔드포인트가 GET → 상태 변경 발생 | ✅ 수정완료 |
| 4 | 🟠 중간 | `AuthService.logout` | 로그아웃 시 Redis에 Lichess/Chess.com 액세스 토큰 잔류 | ✅ 의도된 설계 |
| 5 | 🟠 중간 | `JwtService.generateRefreshToken` | Refresh Token에 플랫폼 정보 없음 → 쿠키명 파싱에 의존 | ✅ 수정완료 |
| 6 | 🟡 낮음 | `ChesscomOAuthService` | 인터페이스 대신 구체 구현체 직접 주입 | ✅ 수정완료 |
| 7 | 🟡 낮음 | `JwtService` | `@Transactional` 오용 | ✅ 수정완료 |
| 8 | 🟡 낮음 | `JwtService.logout()` | 잘못된 쿠키 이름으로 삭제하는 데드코드 | ✅ 수정완료 (#1과 함께) |

---

## 1. 🔴 쿠키 이름 체계 이중화 (로그아웃 실질적 미동작)

### 문제
쿠키 이름을 관리하는 enum이 두 개 공존하며 서로 다른 이름을 사용함.

| Enum | 쿠키 이름 예시 |
|------|--------------|
| `JwtRule` | `ChessLadder-Access`, `ChessLadder-refresh` |
| `CookieName` | `CHESSLADDER_LICHESS_ACCESS`, `CHESSLADDER_CHESSCOM_ACCESS` |

- **실제 쿠키 발급**: `CookieManager` → `CookieName.ACCESS_TOKEN.of(provider)` 사용
- **`JwtService.logout()`**: `JwtUtil.resetToken(ACCESS_PREFIX)` → `ChessLadder-Access` 이름으로 삭제 시도

```java
// JwtService.java:131 - 잘못된 쿠키 이름으로 삭제
public void logout(HttpServletResponse res) {
    res.addCookie(util.resetToken(ACCESS_PREFIX));   // "ChessLadder-Access" (존재하지 않는 쿠키)
    res.addCookie(util.resetToken(REFRESH_PREFIX));  // "ChessLadder-refresh" (존재하지 않는 쿠키)
}
```

- **실제 로그아웃 흐름**(`AuthService` → `LichessLogoutStrategy`)은 `CookieName`을 올바르게 사용하므로 로그아웃 자체는 동작하지만, `JwtService.logout()`은 **데드코드이면서 오동작 코드**임.
- `JwtUtil.resolveTokenFromCookie`도 `JwtRule` 기반이라 실제 쿠키를 못 찾음.

### 해결 방안
- `JwtRule` enum 제거 또는 `CookieName`으로 통일
- `JwtService.logout()` 메서드 제거 (실제 로그아웃은 `LogoutStrategy`에서 처리)

### ✅ 수정 내용 (2026-04-16)

**삭제된 파일**
- `jwt/JwtRule.java` — `ChessLadder-Access` / `ChessLadder-refresh` 라는 잘못된 쿠키 이름을 정의하던 레거시 enum 전체 삭제

**`JwtService.java` 변경 사항**
- `JwtRule` 관련 static import 제거 (`ACCESS_PREFIX`, `REFRESH_PREFIX`)
- `resolveToken(HttpServletRequest, JwtRule)` 메서드 제거 — `JwtRule` 기반으로 쿠키를 탐색하는 데드코드. 실제 토큰 추출은 `JwtAuthenticationFilter`에서 `CookieName`으로 직접 처리.
- `logout(HttpServletResponse)` 메서드 제거 — 실제 존재하지 않는 쿠키명(`ChessLadder-Access`, `ChessLadder-refresh`)으로 삭제를 시도하던 데드코드. 실제 로그아웃은 `AuthService` → `LogoutStrategy`에서 `CookieName`을 사용해 올바르게 처리됨.
- 불필요해진 import 정리: `AuthErrorCode`, `AuthException`, `AuthRedisPrefix`, `RedisService`, `HttpServletRequest`

**`JwtUtil.java` 변경 사항**
- `resolveTokenFromCookie(Cookie[], JwtRule)` 메서드 제거 — `JwtRule` prefix 기반 쿠키 탐색 메서드. 이미 `JwtAuthenticationFilter`는 이 메서드를 사용하지 않고 `CookieName`으로 직접 구현.
- `resetToken(JwtRule)` 메서드 제거 — 잘못된 쿠키명으로 Cookie를 생성하던 메서드. `LogoutStrategy`에서 `AbstractLogoutStrategy.deleteCookie(String, String)`를 직접 사용하므로 불필요.
- `createTokenCookie(JwtRule, ...)` 메서드 제거 — 미사용 메서드. 실제 쿠키 발급은 `CookieManager.addAuthCookies()`에서 처리.
- 불필요해진 `Cookie` import 제거

**결과**
- 쿠키 이름 관리의 단일 출처가 `CookieName` enum으로 통일됨
- `CHESSLADDER_{PLATFORM}_ACCESS` / `CHESSLADDER_{PLATFORM}_REFRESH` 패턴으로 발급·검증·삭제 모두 일관성 확보

---

## 2. 🔴 크로스 플랫폼 ID 충돌 (✅ 수정완료)

### 문제
`lichess_users.id`와 `chesscom_users.id`는 **별개의 auto-increment 시퀀스**이므로 두 테이블에서 같은 id 값이 나올 수 있음.

```java
// 수정 전 - 플랫폼 무관하게 principal.getId()로 필터
SearchUsersResponse response = userService.searchUsers(username, platform, principal.getId());

// 결과: lichess 유저(id=1)가 chesscom 검색 시, chesscom 유저(id=1)도 잘못 제외됨
```

### 수정 내용
```java
// PublicUserController.java - 같은 플랫폼일 때만 본인 제외
Long excludeUserId = platform == principal.getProvider() ? principal.getId() : null;
SearchUsersResponse response = userService.searchUsers(username, platform, excludeUserId);
```

---

## 3. 🟠 refresh 엔드포인트 GET 사용 (REST 위반 + 보안)

### 문제
```java
// AuthController.java:43
@GetMapping("refresh")  // ← GET 요청에서 상태 변경 발생
public ResponseEntity<SuccessResponse<Void>> refreshToken(...) {
    authService.refresh(req, res); // 새 토큰 발급, Redis 갱신
}
```

- GET은 멱등/안전해야 하는 HTTP 메서드이지만, refresh는 토큰 발급 + Redis 업데이트를 수행
- 브라우저 prefetch, 로그 등에 의해 의도치 않게 호출될 수 있음

### ✅ 수정 내용 (2026-04-16)

`AuthController.java`에서 `@GetMapping("refresh")` → `@PostMapping("refresh")`로 변경.  
불필요해진 `GetMapping` import 제거.

---

## 4. ✅ 의도된 설계 — 로그아웃 후 Redis 플랫폼 토큰 잔류

### 배경 (초기 분석 시 문제로 기재됨)
로그인 시 플랫폼 액세스 토큰을 Redis에 저장하지만, 로그아웃 시 삭제하지 않음.

```java
// 로그아웃 시 (AuthService.java:67)
authRedisRepository.deleteRefreshToken(userPrincipal.getId()); // JWT refresh만 삭제
// ← Lichess/Chesscom 액세스 토큰은 TTL 만료 전까지 Redis에 잔류
```

### 의도된 이유
사용자가 로그아웃하더라도 **워커 서버가 플랫폼 API를 통해 게임 데이터를 계속 수집·갱신**해야 하기 때문에 Redis 토큰을 보존함.

워커 동작 확인:
- `ChesscomTokenRefresher` — Redis의 Chess.com Refresh Token으로 Access Token을 재발급하여 Redis에 저장
- `ChessComGameSyncWorker` — `PlatformTokenStore`를 통해 Access Token을 읽어 Chess.com API 호출
- `LichessGameSyncWorker` — `PlatformTokenStore`를 통해 Lichess Access Token으로 API 호출

즉, 로그인/로그아웃 여부와 관계없이 플랫폼 토큰은 Redis TTL이 만료될 때까지 유지되어야 한다. 수정하지 않는다.

### ⚠️ 함께 발견된 버그 (2026-04-16 수정)
초기 로그인 시 Chess.com **Refresh Token TTL을 Access Token의 `expires_in`(24시간)으로 잘못 저장**하던 문제 수정.

```java
// ChesscomOAuthService.java - 수정 전 (잘못된 TTL)
authRedisRepository.saveChesscomRefreshToken(saveUser.getId(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn()); // 86400초 = 24시간

// 수정 후 (30일 고정)
authRedisRepository.saveChesscomRefreshToken(saveUser.getId(), tokenResponse.getRefreshToken(), 30 * 24 * 3600);
```

`PlatformTokenStore.saveChesscomTokens()`(워커 갱신 경로)은 이미 `30 * 24 * 3600`으로 올바르게 저장 중이었으나,  
최초 로그인 경로인 `ChesscomOAuthService.callback()`만 누락되어 있었음.  
초기 로그인 직후부터 30일 TTL이 올바르게 설정된다.

---

## 5. 🟠 Refresh Token에 플랫폼 정보 없음

### 문제
Refresh Token JWT payload에 `provider` 클레임이 없음.

```java
// JwtGenerator.java:43 - provider 정보 없이 생성
return Jwts.builder()
    .setSubject(String.valueOf(id))
    // .claim("provider", provider) ← 없음
    .setExpiration(...)
    .signWith(...)
```

플랫폼 판별을 쿠키 이름(`CHESSLADDER_LICHESS_REFRESH` vs `CHESSLADDER_CHESSCOM_REFRESH`)으로 하는 복잡한 로직에 의존:

```java
// AuthService.java:80-97 - 쿠키 이름에서 플랫폼 파싱
for (Cookie cookie : cookies) {
    for (OAuthPlatForm platform : OAuthPlatForm.values()) {
        String cookieName = CookieName.REFRESH_TOKEN.of(platform);
        if (!cookie.getName().equals(cookieName)) continue;
        ...
    }
}
```

### 해결 방안
Refresh Token 생성 시 `provider` 클레임 포함:
```java
return Jwts.builder()
    .setSubject(String.valueOf(id))
    .claim("provider", provider)  // 추가
    ...
```

### ✅ 수정 내용 (2026-04-16)

**`JwtGenerator.java`**
- `generateRefreshToken(Key, long, Long)` → `generateRefreshToken(Key, long, Long, OAuthPlatForm provider)`로 시그니처 변경
- `.claim("provider", provider)` 추가하여 RT 자체에 플랫폼 정보 포함

**`JwtService.java`**
- `generateRefreshToken(Long id)` → `generateRefreshToken(Long id, OAuthPlatForm provider)`로 시그니처 변경, generator에 전달
- `generateTokenResponse()`에서 `provider`를 `generateRefreshToken()`에 전달
- `getProviderFromRefreshToken(String refreshToken)` 메서드 추가 — RT claims에서 `provider` 클레임을 파싱하여 `OAuthPlatForm` 반환

**`AuthService.refresh()`**
- 기존: 모든 플랫폼 쿠키명을 이중 for-loop으로 순회하며 `detectedProvider`를 쿠키 이름에서 파싱
- 변경: 쿠키 순회는 RT 값을 찾는 용도로만 사용하고, provider는 `jwtService.getProviderFromRefreshToken()`으로 토큰 클레임에서 직접 추출

```java
// 수정 전 — 쿠키명에서 플랫폼 파싱
OAuthPlatForm detectedProvider = null;
for (Cookie cookie : cookies) {
    for (OAuthPlatForm platform : OAuthPlatForm.values()) {
        String cookieName = CookieName.REFRESH_TOKEN.of(platform);
        if (!cookie.getName().equals(cookieName)) continue;
        if (detectedProvider != null && detectedProvider != platform)
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        refreshToken = cookie.getValue();
        detectedProvider = platform;
    }
}

// 수정 후 — 클레임에서 직접 추출
Long userId = Long.parseLong(jwtService.getSubject(refreshToken));
OAuthPlatForm provider = jwtService.getProviderFromRefreshToken(refreshToken);
```

---

## 6. 🟡 인터페이스 대신 구체 구현체 직접 주입

### 문제
```java
// ChesscomOAuthService.java - 구체 구현체 주입 (의존성 역전 원칙 위반)
private final ChesscomUserRepositoryImpl chesscomUserRepository;
```

다른 서비스들은 `ChesscomUserRepository` 인터페이스를 사용하는 반면, `ChesscomOAuthService`만 구현체를 직접 참조.

### ✅ 수정 내용 (2026-04-16)

import를 `ChesscomUserRepositoryImpl` → `ChesscomUserRepository`(도메인 인터페이스)로 교체, 필드 타입 동일하게 변경.

---

## 7. 🟡 `@Transactional` 오용

### 문제
```java
// JwtService.java
@Transactional
public String generateAccessToken(...) { ... }  // DB 접근 없음

@Transactional
public String generateRefreshToken(...) {
    authRedisRepository.saveRefreshToken(...); // Redis 작업 (DB 트랜잭션 무관)
}

@Transactional
public TokenResponse generateTokenResponse(...) { ... }
```

JPA DB 트랜잭션이 불필요한 메서드들. Redis 작업은 Spring `@Transactional` 범위에 포함되지 않음.

### ✅ 수정 내용 (2026-04-16)

세 메서드에서 `@Transactional` 어노테이션 및 관련 import 제거.

---

## 8. 🟡 `JwtService.logout()` 데드코드 (✅ #1과 함께 수정완료)

### 문제
```java
// JwtService.java - 호출되지 않는 메서드
@Transactional
public void logout(HttpServletResponse res) {
    res.addCookie(util.resetToken(ACCESS_PREFIX));   // 잘못된 쿠키명 "ChessLadder-Access"
    res.addCookie(util.resetToken(REFRESH_PREFIX));  // 잘못된 쿠키명 "ChessLadder-refresh"
}
```

실제 로그아웃은 `AuthService` → `LogoutStrategy`로 처리되므로 이 메서드는 사용되지 않음.  
호출되더라도 실제 쿠키 이름과 달라서 쿠키 삭제가 되지 않음.

### ✅ 수정 내용

`#1` 수정 시 `JwtRule` enum 제거 및 관련 메서드 정리 과정에서 함께 제거됨.

---

## 수정 우선순위

```
🔴 즉시 수정  → #1 쿠키 이름 통일 (완료), #2 크로스 플랫폼 ID (완료)
🟠 다음 스프린트 → #3 refresh POST 변경 (완료), #4 의도된 설계 확인 (완료), #5 RT provider 클레임 (완료)
🟡 리팩토링 시 → #6 인터페이스 주입 (완료), #7 @Transactional 제거 (완료), #8 데드코드 제거 (완료)
```
