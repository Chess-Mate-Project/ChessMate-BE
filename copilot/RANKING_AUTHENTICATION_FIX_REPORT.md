# 랭킹 API 인증 문제 해결 보고서

**작성 일시:** 2026-02-13  
**이슈:** `/api/rank/ranking` 요청 시 인증된 사용자의 `userPrincipal`이 null인 문제  
**상태:** ✅ 해결 완료

---

## 🔍 문제 분석

### 원인
**JwtAuthenticationFilter의 필터 제외 경로 설정 오류**

```java
// ❌ 문제 있는 코드
String[] excluded = {
    "/api/oauth/oauth-url",
    "/api/oauth/callback",
    "/api/auth/refresh",
    "/api/user/count",
    "/api/rank/ranking"  // ← 이 경로가 필터에서 제외됨
};
```

### 영향
- `/api/rank/ranking`이 필터 제외 경로에 등록되어 있었음
- JWT 토큰 검증 필터가 실행되지 않음
- SecurityContext에 인증 정보가 설정되지 않음
- 인증된 사용자라도 `userPrincipal`이 null로 전달됨

### 왜 이렇게 설정되었는가?
비인증 사용자도 랭킹을 조회할 수 있도록 의도한 것으로 보임. 하지만 **인증과 인가(접근 제어)를 혼동한 실수**:
- **인증(Authentication):** 사용자가 누구인지 확인
- **인가(Authorization):** 해당 사용자가 리소스에 접근할 권한이 있는지 확인

---

## ✅ 해결 방법

### 1. 필터 제외 경로에서 `/api/rank/ranking` 제거

```java
// ✅ 수정된 코드
String[] excluded = {
    "/api/oauth/oauth-url",
    "/api/oauth/callback",
    "/api/auth/refresh",
    "/api/user/count"
    // "/api/rank/ranking" 제거
};
```

**이유:**
- 인증된 사용자의 정보를 필터에서 검증하기 위해 필터가 항상 실행되어야 함
- 비인증 사용자의 경우, RankService에서 `userPrincipal == null`로 처리
- 필터 실행 → 인증 정보 검증 → RankService에서 null 체크하는 흐름이 정상

### 2. JWT 필터의 로깅 강화

```java
// ✅ 추가된 로그
@Override
public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
    String uri = request.getRequestURI();
    log.info("[JWT Filter] 요청 URI: {}", uri);

    String accessToken = jwtService.resolveToken(request, JwtRule.ACCESS_PREFIX);
    if (accessToken == null || accessToken.isBlank()) {
        log.info("[JWT Filter] 엑세스 토큰 없음 - 비인증 사용자로 처리: {}", uri);
        chain.doFilter(request, response);
        return;
    }

    log.info("[JWT Filter] 토큰 검증 시작 - uri={}", uri);
    if (jwtService.validateAccessToken(accessToken)) {
        SecurityContextHolder.getContext().setAuthentication(
                jwtService.getAuthentication(accessToken)
        );
        log.info("[JWT Filter] 엑세스 토큰 검증 성공, 인증 객체 설정 완료 - uri={}", uri);
        chain.doFilter(request, response);
        return;
    }

    log.warn("[JWT Filter] 엑세스 토큰 검증 실패 - uri={}", uri);
    chain.doFilter(request, response);
}
```

### 3. 필터 제외 로직 개선

```java
protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null) return true;

    // 필터에서 제외할 경로들 - 인증이 필요 없는 공개 엔드포인트만 추가
    String[] excluded = {
        "/api/oauth/oauth-url",
        "/api/oauth/callback",
        "/api/auth/refresh",
        "/api/user/count"
    };

    for (String path : excluded) {
      if (uri.startsWith(path)) {
        log.debug("[Filter Skip] 필터 제외 경로 - uri={}", uri);
        return true;
      }
    }
    
    if (request.getMethod().equals("OPTIONS")) {
      log.debug("[Filter Skip] OPTIONS 요청 - uri={}", uri);
      return true;
    }
    return false;
}
```

### 4. 불필요한 코드 제거

```java
// ❌ 제거된 코드
private final UserRepositoryImpl userRepository;  // 사용하지 않음

import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;  // 미사용 import
```

---

## 📊 요청 흐름도

### 인증된 사용자 요청
```
요청: GET /api/rank/ranking
  ↓
JwtAuthenticationFilter (필터 적용)
  ├─ 토큰 추출
  ├─ 토큰 검증
  └─ SecurityContext에 인증 정보 설정 ✅
  ↓
RankController
  ├─ userPrincipal != null ✅
  └─ RankService에 user 정보 전달
  ↓
RankService
  ├─ user != null 확인
  ├─ 사용자의 랭킹 정보 조회
  └─ 응답 반환
```

### 비인증 사용자 요청
```
요청: GET /api/rank/ranking
  ↓
JwtAuthenticationFilter (필터 적용)
  ├─ 토큰 없음 감지
  └─ 필터 통과 (인증 정보 설정 안함)
  ↓
RankController
  ├─ userPrincipal == null
  └─ RankService에 null 전달
  ↓
RankService
  ├─ user == null 확인
  ├─ 비인증 사용자 응답 생성
  │  (내 순위 정보: null, 랭킹: 게스트)
  └─ 응답 반환
```

---

## 📝 로그 추적 예시

### 인증된 사용자 요청 (성공)
```
[JWT Filter] 요청 URI: /api/rank/ranking
[JWT Filter] 토큰 검증 시작 - uri=/api/rank/ranking
[JWT Filter] 엑세스 토큰 검증 성공, 인증 객체 설정 완료 - uri=/api/rank/ranking
[Ranking API] gameType=RAPID, page=0, size=20, isAuthenticated=true
[Ranking Request] gameType=RAPID, page=0, pageSize=20, user=john_doe
[Cache-Hit] Ranking 캐시 조회 성공 - gameType=RAPID
[Pagination] totalCount=1000, pageSize=20, currentPage=0, totalPages=50, startIndex=0, endIndex=20
[Ranking Complete] gameType=RAPID, page=0, rankerCount=20, totalPages=50
[Ranking Response] gameType=RAPID, page=0, rankerCount=20, totalPages=50
```

### 비인증 사용자 요청 (성공)
```
[JWT Filter] 요청 URI: /api/rank/ranking
[JWT Filter] 엑세스 토큰 없음 - 비인증 사용자로 처리: /api/rank/ranking
[Ranking API] gameType=RAPID, page=0, size=20, isAuthenticated=false
[Ranking Request] gameType=RAPID, page=0, pageSize=20, user=Anonymous
[Ranking] 비로그인 사용자 - 게스트 랭킹 제공 gameType=RAPID, page=0
[Ranking Response] gameType=RAPID, page=0, rankerCount=0, totalPages=0
```

### 토큰 검증 실패 (경고)
```
[JWT Filter] 요청 URI: /api/rank/ranking
[JWT Filter] 토큰 검증 시작 - uri=/api/rank/ranking
[JWT Filter] 엑세스 토큰 검증 실패 - uri=/api/rank/ranking
[Ranking API] gameType=RAPID, page=0, size=20, isAuthenticated=false
```

---

## 🎯 RankController에서의 처리

```java
@GetMapping("/ranking")
public ResponseEntity<SuccessResponse<RankingResponse>> getRanking(
    @AuthenticationPrincipal UserPrincipal userPrincipal,
    @RequestParam(defaultValue = "RAPID") GameType gameType,
    @PageableDefault(size = 20) Pageable pageable
) {
    // userPrincipal이 정상적으로 인증된 사용자 정보를 가지고 있음
    RankingResponse response = rankService.getRankers(
        userPrincipal != null ? userPrincipal.getUser() : null,  // 이제 정상 작동
        gameType,
        pageable
    );
    
    return ResponseEntity.ok(new SuccessResponse<>("Ranking 조회 성공", response));
}
```

---

## 📋 변경 사항 요약

| 항목 | 변경 전 | 변경 후 |
|------|--------|--------|
| `/api/rank/ranking` 필터 제외 | ✅ 제외 | ❌ 필터 적용 |
| JWT 검증 실행 여부 | ❌ 안 함 | ✅ 함 |
| 인증 정보 설정 | ❌ 설정 안 함 | ✅ 설정 함 |
| userPrincipal 값 | null | 인증된 사용자 정보 |
| 로그 상세도 | 낮음 | 높음 |
| 미사용 필드 제거 | userRepositoryImpl 미사용 | 제거됨 |

---

## ✨ 개선 효과

### Before (문제 상황)
```
인증된 사용자가 /api/rank/ranking 요청
  → JWT 필터 스킵
  → userPrincipal = null
  → "비인증 사용자" 취급
  → 내 순위 조회 불가
```

### After (해결 후)
```
인증된 사용자가 /api/rank/ranking 요청
  → JWT 필터 실행 및 토큰 검증
  → userPrincipal = 사용자 정보
  → "인증된 사용자" 확인
  → 내 순위 조회 가능 ✅
```

---

## 🚀 결론

JWT 인증 필터 설정의 오류로 인해 `/api/rank/ranking` 엔드포인트에서 인증된 사용자의 정보가 전달되지 않는 문제를 해결했습니다.

**핵심:**
- 인증과 인가는 분리되어야 함
- 필터는 항상 실행되어 인증 정보를 검증하고
- 비즈니스 로직(Service)에서 인증 정보의 null 여부를 확인하여 접근 제어 수행

이제 인증된 사용자와 비인증 사용자 모두 올바르게 처리됩니다.


