# Spring Security OAuth2 마이그레이션 가이드

## 📋 개요

기존 직접 API 호출 방식의 OAuth를 **Spring Security OAuth2 프레임워크**로 전환했습니다.

---

## 🔄 전체 인증 흐름

```
┌──────────────┐
│   클라이언트   │
└──────┬───────┘
       │
       │ 1. OAuth 로그인 클릭
       │    /oauth2/authorization/lichess
       ↓
┌──────────────────────────────┐
│  Spring Security OAuth2       │
│  (자동 리다이렉트 처리)         │
└──────┬───────────────────────┘
       │
       │ 2. Lichess OAuth Server로 리다이렉트
       │    https://lichess.org/oauth?...
       ↓
┌──────────────────────────────┐
│   Lichess OAuth Server       │
│   (사용자 인증)               │
└──────┬───────────────────────┘
       │
       │ 3. Authorization Code 반환
       │    http://localhost:8080/login/oauth2/code/lichess?code=xxx&state=yyy
       ↓
┌──────────────────────────────────────────┐
│  CustomOAuth2UserService.loadUser()      │
│  1. 사용자 정보 로드                      │
│  2. DB 저장/업데이트                      │
│  3. OAuth 토큰 Redis 저장                │
│  4. 배치 작업 트리거                      │
└──────┬───────────────────────────────────┘
       │
       │ 4. OAuth2PrincipalDetails 반환
       ↓
┌──────────────────────────────────────────┐
│  OAuth2SuccessHandler.onAuthenticationSuccess() │
│  1. 서버 accessToken 발급                 │
│  2. 서버 refreshToken 발급 + Redis 저장   │
│  3. 쿠키 설정 (HttpOnly, Secure)         │
└──────┬───────────────────────────────────┘
       │
       │ 5. 클라이언트로 리다이렉트
       │    Set-Cookie 헤더 포함
       ↓
┌──────────────┐
│   클라이언트   │
│ (로그인 성공) │
└──────────────┘
```

---

## 🔐 핵심 컴포넌트

### 1️⃣ CustomOAuth2UserService
**파일**: `chessmate-api/src/main/java/.../oauth/CustomOAuth2UserService.java`

**역할**: OAuth 제공자로부터 사용자 정보 로드 및 DB 저장

```java
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
  
  @Override
  public OAuth2User loadUser(OAuth2UserRequest request) {
    // 1. OAuth 제공자로부터 사용자 정보 로드
    OAuth2User oauthUser = delegate.loadUser(request);
    
    // 2. 제공자별 사용자 정보 처리
    User user = handleOAuthLogin(registrationId, oauthUser, attributes);
    
    // 3. OAuth 토큰 Redis 저장
    cacheService.saveLichessToken(user.getId(), accessToken);
    
    // 4. 배치 작업 트리거
    lichessApiProducer.sendSyncTask(user, username, accessToken, TaskType.PERF, true);
    
    // 5. OAuth2PrincipalDetails 반환
    return new OAuth2PrincipalDetails(user, attributes);
  }
}
```

**핵심 메서드**:
- `handleLichessLogin()`: Lichess 사용자 처리
  - 신규 사용자 → DB 저장
  - 기존 사용자 → 업데이트 (lastLoginAt, 통계 등)
- `handleChessComLogin()`: Chess.com 사용자 처리 (향후 구현)

---

### 2️⃣ OAuth2SuccessHandler
**파일**: `chessmate-api/src/main/java/.../oauth/OAuth2SuccessHandler.java`

**역할**: OAuth2 인증 성공 후 서버 토큰 발급 및 리다이렉트

```java
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
  
  private final JwtService jwtService;
  
  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, 
      HttpServletResponse response,
      Authentication authentication) {
    
    // 1. OAuth2 인증 사용자 추출
    OAuth2PrincipalDetails details = (OAuth2PrincipalDetails) authentication.getPrincipal();
    User user = details.getUser();
    
    // 2. 서버 JWT 토큰 발급
    String accessToken = jwtService.generateAccessToken(response, user);
    String refreshToken = jwtService.generateRefreshToken(response, user);
    
    // 3. 클라이언트로 리다이렉트
    // (쿠키는 HTTP 응답 헤더의 Set-Cookie에 자동 추가)
    getRedirectStrategy().sendRedirect(request, response, clientUrl);
  }
}
```

**역할**:
- `accessToken`: 짧은 만료시간 (10초)
  - 응답 헤더: `Set-Cookie: access_token=xxx; Path=/; Domain=localhost; Secure; HttpOnly=false; SameSite=Lax; Max-Age=10`
  
- `refreshToken`: 긴 만료시간 (7일 = 604800초)
  - 응답 헤더: `Set-Cookie: refresh_token=yyy; Path=/; Domain=localhost; Secure; HttpOnly=true; SameSite=Lax; Max-Age=604800`
  - Redis: `refresh_token:{userId}` → `refreshToken`

---

### 3️⃣ SecurityConfig
**파일**: `chessmate-api/src/main/java/.../auth/SecurityConfig.java`

**역할**: Spring Security 설정 (OAuth2, JWT, CORS 등)

```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
  
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // JWT 기반 → Stateless
        )
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/oauth/oauth-url", "/oauth2/authorization/lichess")
            .permitAll()  // OAuth 엔드포인트는 인증 불필요
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(user -> user.userService(customOAuth2UserService))
            .successHandler(oAuth2SuccessHandler)  // OAuth2 성공 시 토큰 발급
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
```

**설정 항목**:
- **sessionCreationPolicy.STATELESS**: JWT 기반 인증이므로 세션 비활성화
- **oauth2Login()**: OAuth2 인증 활성화
- **jwtAuthenticationFilter**: JWT 토큰 검증 필터 추가

---

## 📡 엔드포인트

### 1. OAuth URL 생성 (PKCE 포함)
```http
GET /api/oauth/oauth-url
```

**응답**:
```json
{
  "message": "OAuth URL 생성 완료",
  "data": {
    "oauth_url": "https://lichess.org/oauth?code_challenge_method=S256&code_challenge=xxx&response_type=code&client_id=ChessLadder&redirect_uri=http://localhost:8080/login/oauth2/code/lichess&state=yyy"
  }
}
```

---

### 2. OAuth2 인증 자동 처리
```http
GET /oauth2/authorization/lichess
```

**Spring Security가 자동으로 처리**:
1. Lichess OAuth Server로 리다이렉트
2. 사용자 인증
3. Authorization Code 반환
4. `CustomOAuth2UserService.loadUser()` 호출
5. `OAuth2SuccessHandler.onAuthenticationSuccess()` 호출
6. 클라이언트로 리다이렉트 + 토큰 발급

---

### 3. 로그인 콜백 (Spring Security 자동 처리)
```http
GET /login/oauth2/code/lichess?code=xxx&state=yyy
```

**처리 흐름**:
1. Authorization Code 검증
2. Access Token 발급 (Lichess)
3. 사용자 정보 조회 (Lichess)
4. `CustomOAuth2UserService` 처리
5. JWT 발급
6. 클라이언트 리다이렉트

---

## 🎯 데이터 저장소

### Redis (OAuth 토큰)
```
Key: lichess_token:{userId}
Value: <Lichess OAuth Access Token>
TTL: 1시간 (또는 설정값)

용도: Lichess API 호출 시 인증
```

### Redis (서버 Refresh Token)
```
Key: refresh_token:{userId}
Value: <서버 JWT Refresh Token>
TTL: 7일 (REFRESH_EXP)

용도: Access Token 재발급 시 검증
```

### DB (사용자 정보)
```
users 테이블
- id (PK)
- lichess_id (OAuth 제공자의 고유 ID)
- username
- title
- all_games, rated_games, wins, losses, draws
- last_login_at
- created_at
```

---

## 🔑 JWT 토큰

### Access Token (accessToken_)
- **만료시간**: 10초 (설정: `spring.jwt.access-token.expiration`)
- **저장 위치**: HTTP 쿠키 + 프론트 localStorage
- **쿠키 속성**:
  - `HttpOnly=false` (JavaScript 접근 가능)
  - `Secure=true` (HTTPS only)
  - `SameSite=Lax` (CSRF 방지)
- **용도**: API 요청 인증

### Refresh Token (refreshToken_)
- **만료시간**: 7일 (설정: `spring.jwt.refresh-token.expiration`)
- **저장 위치**: HTTP 쿠키만 (프론트는 접근 불가)
- **쿠키 속성**:
  - `HttpOnly=true` (JavaScript 접근 불가)
  - `Secure=true` (HTTPS only)
  - `SameSite=Lax` (CSRF 방지)
- **용도**: Access Token 갱신

---

## 🛠️ 프론트엔드 구현 (상세 명세)

### 1. OAuth 로그인 버튼 클릭
```typescript
// 방법 1: Spring Security 자동 처리
const loginWithLichess = () => {
  window.location.href = '/oauth2/authorization/lichess';
  // 또는
  window.location.href = 'http://localhost:8080/oauth2/authorization/lichess';
};

// 방법 2: PKCE 수동 처리 (고급)
const loginWithPKCE = async () => {
  const response = await fetch('http://localhost:8080/api/oauth/oauth-url');
  const data = await response.json();
  window.location.href = data.data.oauth_url;
};
```

---

### 2. 로그인 콜백 처리
```typescript
// /oauth/callback 또는 /login 페이지에서
useEffect(() => {
  // 1. URL 쿼리 파라미터 확인 (error 확인)
  const params = new URLSearchParams(window.location.search);
  const error = params.get('error');
  
  if (error) {
    console.error('OAuth 실패:', error);
    return;
  }
  
  // 2. 서버가 리다이렉트 완료 (쿠키 자동 설정됨)
  // 3. JWT 토큰 확인
  const accessToken = getCookieValue('accessToken_'); // 또는 localStorage
  const refreshToken = getCookieValue('refreshToken_');
  
  if (accessToken && refreshToken) {
    // 4. localStorage에 accessToken 저장 (선택)
    localStorage.setItem('accessToken', accessToken);
    
    // 5. 대시보드로 리다이렉트
    navigate('/dashboard');
  }
}, []);
```

---

### 3. API 요청 시 토큰 전송
```typescript
// axios interceptor 또는 fetch wrapper
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  withCredentials: true,  // 쿠키 자동 포함
});

apiClient.interceptors.request.use((config) => {
  // 1. 쿠키에서 accessToken 또는 localStorage에서 가져오기
  const accessToken = localStorage.getItem('accessToken');
  
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  
  return config;
});

// 사용
const response = await apiClient.get('/user/profile');
```

---

### 4. Access Token 갱신 (자동)
```typescript
// 서버가 401 응답 시
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // 1. Refresh Token 검증 (쿠키에 자동 포함)
      const refreshResponse = await fetch(
        'http://localhost:8080/api/auth/refresh',
        {
          method: 'POST',
          credentials: 'include', // 쿠키 포함
        }
      );
      
      if (refreshResponse.ok) {
        // 2. 새 accessToken 받음 (쿠키로 반환)
        const data = await refreshResponse.json();
        localStorage.setItem('accessToken', data.data.accessToken);
        
        // 3. 원래 요청 재시도
        return apiClient(error.config);
      } else {
        // 4. 갱신 실패 → 로그인 페이지로 이동
        navigate('/login');
      }
    }
    
    return Promise.reject(error);
  }
);
```

---

### 5. 로그아웃
```typescript
const logout = async () => {
  // 1. 서버에 로그아웃 요청
  await fetch('http://localhost:8080/api/auth/logout', {
    method: 'POST',
    credentials: 'include', // 쿠키 포함 (refreshToken 검증용)
  });
  
  // 2. 로컬 상태 정리
  localStorage.removeItem('accessToken');
  localStorage.removeItem('user');
  
  // 3. 로그인 페이지로 이동
  navigate('/login');
};
```

---

## 🐛 디버깅

### 1. 쿠키 확인
```javascript
// 브라우저 개발자 도구 > Application > Cookies
// 확인 사항:
// - accessToken_: 있는가? (HttpOnly=false면 JavaScript 접근 가능)
// - refreshToken_: 있는가? (HttpOnly=true면 브라우저가 자동 관리)
// - Domain: localhost 또는 .chessladder.org인가?
// - Path: / 인가?
// - Secure: HTTPS에서 true인가?
// - SameSite: Lax 또는 None인가?

document.cookie // JavaScript에서 확인 (HttpOnly=false 쿠키만)
```

### 2. JWT 토큰 디코딩
```javascript
// https://jwt.io에서 확인
const token = localStorage.getItem('accessToken');
console.log(token); // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

// 또는 터미널에서
echo "token" | jq -R 'split(".")[1] | @base64d'
```

### 3. CORS 오류 해결
```
Access to fetch at 'http://localhost:8080/api/...' 
from origin 'http://localhost:5173' has been blocked by CORS policy
```

**원인**: SecurityConfig의 CORS 설정 부족

**해결**:
```yaml
# application-api.yml
spring:
  web:
    cors:
      allowed-origins:
        - http://localhost:5173
        - http://localhost:5174
        - https://chessladder.org
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
        - OPTIONS
      allowed-headers:
        - "*"
      allow-credentials: true
      max-age: 3600
```

---

## 📝 설정 파일

### application-api.yml (OAuth2 설정)
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          lichess:
            client-id: ChessLadder
            client-authentication-method: none  # PKCE 자동 활성화
            authorization-grant-type: authorization_code
            redirect-uri: http://localhost:8080/login/oauth2/code/lichess
            scope:
              - "preference:read"
              - "email:read"
            client-name: Lichess
        provider:
          lichess:
            authorization-uri: https://lichess.org/oauth
            token-uri: https://lichess.org/api/token
            user-info-uri: https://lichess.org/api/account
            user-name-attribute: id  # 사용자 고유 ID 필드

  jwt:
    access-token:
      secret: chessMateGbswProjectAccessTokenSecret
      expiration: 10000  # 10초 (밀리초)
    refresh-token:
      secret: chessMateGbswProjectRefreshTokenSecret
      expiration: 604800000  # 7일 (밀리초)

cookie:
  domain: localhost  # 프로덕션: .chessladder.org

client:
  url: http://localhost:5173  # 프로덕션: https://chessladder.org
```

---

## ✅ 체크리스트

- [ ] CustomOAuth2UserService 구현
- [ ] OAuth2SuccessHandler 구현
- [ ] SecurityConfig 수정
- [ ] application-api.yml 설정 확인
- [ ] 프론트: OAuth 로그인 버튼 구현
- [ ] 프론트: 토큰 저장/관리 구현
- [ ] 프론트: API 요청 시 토큰 전송
- [ ] 프론트: 토큰 갱신 로직 구현
- [ ] 프론트: 로그아웃 구현
- [ ] 로컬 테스트 (http://localhost:5173)
- [ ] 스테이징 테스트 (https://chessladder.org)
- [ ] 프로덕션 배포

---

## 🚀 배포 체크리스트

### 프로덕션 환경 설정 (application-prod.yml)
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          lichess:
            redirect-uri: https://chessladder.org/login/oauth2/code/lichess

cookie:
  domain: .chessladder.org  # 서브도메인 공유

client:
  url: https://chessladder.org

# 또는 환경 변수
# COOKIE_DOMAIN=.chessladder.org
# CLIENT_URL=https://chessladder.org
# LICHESS_CLIENT_ID=ChessLadder
```

### 환경 변수 설정 (EC2/Docker)
```bash
export COOKIE_DOMAIN=.chessladder.org
export CLIENT_URL=https://chessladder.org
export LICHESS_CLIENT_ID=ChessLadder
export DB_URL=jdbc:mysql://...
export DB_USERNAME=...
export DB_PASSWORD=...
export REDIS_HOST=...
export REDIS_PASSWORD=...
```

---

## 📚 참고 자료

- [Spring Security OAuth2 공식 문서](https://spring.io/projects/spring-security-oauth2-client)
- [Lichess OAuth2 API](https://lichess.org/api#operation/apiAccountGet)
- [JWT.io](https://jwt.io)
- [PKCE (RFC 7636)](https://tools.ietf.org/html/rfc7636)

