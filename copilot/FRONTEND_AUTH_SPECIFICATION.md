# 🔐 ChessMate 프론트엔드 인증 구조 상세 명세서

> 본 문서는 프론트엔드 개발자가 ChessMate 백엔드의 인증 시스템을 완전히 이해하고 구현할 수 있도록 작성되었습니다.
> 각 단계별로 상세히 설명되어 있으므로, 이 순서대로 따라가면 됩니다.

---

## 📋 목차

1. [인증 흐름 전체 개요](#인증-흐름-전체-개요)
2. [Step 1: OAuth 시작 페이지 구성](#step-1-oauth-시작-페이지-구성)
3. [Step 2: OAuth URL 요청](#step-2-oauth-url-요청)
4. [Step 3: Lichess OAuth 리다이렉트](#step-3-lichess-oauth-리다이렉트)
5. [Step 4: OAuth Callback 처리](#step-4-oauth-callback-처리)
6. [Step 5: 토큰 저장 및 관리](#step-5-토큰-저장-및-관리)
7. [Step 6: API 요청 시 토큰 포함](#step-6-api-요청-시-토큰-포함)
8. [Step 7: 토큰 갱신 (Refresh)](#step-7-토큰-갱신-refresh)
9. [Step 8: 로그아웃](#step-8-로그아웃)
10. [쿠키 구조 상세 설명](#쿠키-구조-상세-설명)
11. [에러 처리](#에러-처리)
12. [보안 고려사항](#보안-고려사항)

---

## 🔄 인증 흐름 전체 개요

```
프론트엔드                           백엔드                              Lichess OAuth
    |                               |                                    |
    |--1. OAuth URL 요청----------->|                                    |
    |<--2. OAuth URL 반환-----------|                                    |
    |                               |                                    |
    |--3. 사용자를 Lichess로 리다이렉트--->|                                    |
    |                               |--4. Lichess 인증 페이지 표시-------->|
    |                               |                                    |
    |<--5. Authorization Code-------(사용자 승인)                          |
    |                               |<--6. Code + State 반환------------|
    |                               |                                    |
    |--7. Callback 요청------------>|                                    |
    |   (code + state)             |--8. Token 교환 요청----------------->|
    |                               |<--9. Access Token 반환------------|
    |<--10. JWT 토큰 (Cookie)-------|--10. 사용자 정보 조회----------------->|
    |   + Set-Cookie 헤더          |<--11. 사용자 정보 반환------------|
    |                               |                                    |
    |--12. /oauth/success로 리다이렉트-->|
```

---

## Step 1: OAuth 시작 페이지 구성

### 1-1. 사용자가 보는 화면

프론트엔드에 "Lichess로 로그인" 버튼이 있어야 합니다.

```html
<!-- 예시 -->
<button onclick="handleLichessLogin()">
  Lichess로 로그인
</button>
```

### 1-2. 버튼 클릭 시 동작

```javascript
async function handleLichessLogin() {
  // Step 2로 진행
  await getOauthUrl();
}
```

---

## Step 2: OAuth URL 요청

### 2-1. 백엔드 엔드포인트

- **URL**: `GET /api/oauth/oauth-url`
- **인증**: 필요 없음 (공개 엔드포인트)
- **Request Body**: 없음
- **Content-Type**: 자동 (GET 요청)

### 2-2. 프론트엔드 구현 코드

```javascript
async function getOauthUrl() {
  try {
    // 1단계: OAuth URL을 백엔드에서 가져옵니다
    const response = await fetch(
      'http://localhost:8080/api/oauth/oauth-url',
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include' // 쿠키 포함 (필수)
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    // 2단계: JSON 응답 파싱
    const data = await response.json();
    
    // 3단계: 응답 구조 확인
    // data 형태:
    // {
    //   "code": 200,
    //   "message": "OauthUrl 제공",
    //   "data": {
    //     "oauthUrl": "https://lichess.org/oauth?..."
    //   }
    // }

    const oauthUrl = data.data.oauthUrl;
    
    console.log('OAuth URL:', oauthUrl);
    
    // 4단계: Step 3로 진행
    redirectToLichess(oauthUrl);

  } catch (error) {
    console.error('OAuth URL 요청 실패:', error);
    // 에러 처리 로직
  }
}
```

### 2-3. 응답 형식

```json
{
  "code": 200,
  "message": "OauthUrl 제공",
  "data": {
    "oauthUrl": "https://lichess.org/oauth?code_challenge_method=S256&code_challenge=xxxxx&response_type=code&client_id=xxxxx&redirect_uri=xxxxx&state=xxxxx"
  }
}
```

---

## Step 3: Lichess OAuth 리다이렉트

### 3-1. 사용자를 Lichess로 리다이렉트

```javascript
function redirectToLichess(oauthUrl) {
  // Lichess OAuth 페이지로 사용자를 리다이렉트합니다
  // 사용자는 Lichess 로그인 페이지를 보게 됩니다
  window.location.href = oauthUrl;
}
```

### 3-2. 사용자 경험 (UX)

1. 프론트엔드의 "Lichess로 로그인" 버튼을 클릭합니다.
2. Lichess 로그인 페이지로 이동합니다.
3. 사용자가 Lichess 계정으로 로그인합니다.
4. Lichess가 "ChessMate이 당신의 정보에 접근하길 원합니다" 동의 화면을 표시합니다.
5. 사용자가 "승인"을 클릭합니다.
6. Lichess는 사용자를 우리의 callback URL로 리다이렉트합니다 (자동).

---

## Step 4: OAuth Callback 처리

### 4-1. Callback URL 설정

Lichess OAuth 설정에서 다음 URL을 Redirect URI로 등록되어 있습니다:
```
http://localhost:8080/api/oauth/callback
```

실제 배포 시:
```
https://yourdomain.com/api/oauth/callback
```

### 4-2. Lichess가 보내는 요청

Lichess는 다음과 같은 형태로 callback URL을 호출합니다:

```
GET http://localhost:8080/api/oauth/callback?code=xxxxx&state=xxxxx
```

### 4-3. 백엔드 엔드포인트

- **URL**: `GET /api/oauth/callback`
- **Query Parameters**:
  - `code` (string): Lichess가 발급한 Authorization Code
  - `state` (string): 보안을 위한 state 값 (Step 2에서 생성됨)
- **인증**: 필요 없음
- **내부 동작**:
  1. state 값으로 PKCE code_verifier 조회 (Redis에서)
  2. state가 유효한지 검증
  3. code를 사용하여 Lichess에서 access_token 요청
  4. access_token으로 사용자 정보 조회
  5. 사용자가 신규이면 DB에 저장
  6. JWT 토큰 발급 (Access Token + Refresh Token)
  7. **클라이언트를 `/oauth/success`로 리다이렉트**

### 4-4. 응답

백엔드는 HTTP 302 리다이렉트 응답을 보냅니다:

```
HTTP/1.1 302 Found
Location: http://localhost:3000/oauth/success
Set-Cookie: access_token_chessmate=xxxxx; Path=/; Secure; SameSite=None; Max-Age=60
Set-Cookie: refresh_token_chessmate=xxxxx; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=2592000
```

### 4-5. 프론트엔드 최적화

이 단계는 **백엔드가 자동으로 처리**하므로, 프론트엔드는 특별한 작업이 필요 없습니다.
하지만 `/oauth/success` 페이지를 준비해야 합니다 (다음 Step 참고).

---

## Step 5: 토큰 저장 및 관리

### 5-1. Callback 후 프론트엔드 상태

사용자가 `/oauth/success` 페이지에 도달했을 때:

- **Access Token**: 쿠키에 자동으로 저장됨 (이미 Set-Cookie 헤더로 전송됨)
- **Refresh Token**: 쿠키에 자동으로 저장됨 (이미 Set-Cookie 헤더로 전송됨)

### 5-2. `/oauth/success` 페이지 구성

```javascript
// pages/oauth-success.jsx (또는 .tsx)

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function OAuthSuccess() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const handleOAuthSuccess = async () => {
      try {
        // 1단계: 백엔드에서 현재 로그인한 사용자 정보 확인
        const response = await fetch(
          'http://localhost:8080/api/auth/me',
          {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
            },
            credentials: 'include' // 쿠키 포함 (필수!)
          }
        );

        if (!response.ok) {
          throw new Error('사용자 정보 조회 실패');
        }

        const data = await response.json();
        const user = data.data; // User 객체

        console.log('로그인한 사용자:', user);

        // 2단계: 로컬 상태 관리에 사용자 정보 저장
        // (Redux, Zustand, Context API 등 사용)
        localStorage.setItem('user', JSON.stringify(user));

        // 3단계: Access Token을 로컬 스토리지에 따로 저장
        // (선택 사항: 쿠키에 이미 있지만, 필요시 로컬 스토리지에도 저장)
        // const accessToken = getCookieValue('access_token_chessmate');
        // localStorage.setItem('accessToken', accessToken);

        setIsLoading(false);

        // 4단계: 홈 페이지로 리다이렉트 (짧은 딜레이)
        setTimeout(() => {
          navigate('/');
        }, 1000);

      } catch (err) {
        console.error('OAuth 성공 처리 실패:', err);
        setError(err.message);
        setIsLoading(false);
      }
    };

    handleOAuthSuccess();
  }, [navigate]);

  if (isLoading) {
    return <div>로그인 처리 중...</div>;
  }

  if (error) {
    return <div>오류: {error}</div>;
  }

  return null;
}
```

### 5-3. 쿠키에서 값 추출하는 유틸리티 함수

```javascript
// utils/cookieUtils.js

export function getCookieValue(name) {
  // document.cookie에서 특정 이름의 쿠키 값을 가져옵니다
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop().split(';').shift();
  }
  return null;
}

export function getAllCookies() {
  // 모든 쿠키를 객체 형태로 반환합니다
  const cookies = {};
  document.cookie.split(';').forEach(cookie => {
    const [name, value] = cookie.trim().split('=');
    if (name) {
      cookies[name] = decodeURIComponent(value);
    }
  });
  return cookies;
}
```

### 5-4. 토큰 저장 전략 (권장)

| 저장소 | 토큰 | 장점 | 단점 |
|--------|------|------|------|
| **HttpOnly Cookie** | Refresh Token | XSS 공격으로부터 안전 | XHR 요청 시 자동 전송 안 됨 |
| **Memory / State** | Access Token | 신속한 접근 | 페이지 새로고침 시 손실 |
| **LocalStorage** | Access Token | 지속성 | XSS 공격에 취약 |

**권장 전략**:
- ✅ **Refresh Token**: HttpOnly 쿠키에 자동 저장 (백엔드가 처리)
- ✅ **Access Token**: 
  - 방법 1: 쿠키에서 필요할 때마다 읽기 (권장)
  - 방법 2: 메모리 변수에 저장하고 필요시 갱신

---

## Step 6: API 요청 시 토큰 포함

### 6-1. Access Token 쿠키 이름

- **쿠키 이름**: `access_token_chessmate`
- **maxAge**: 60초 (1분)
- **HttpOnly**: false (프론트엔드에서 읽을 수 있음)
- **Secure**: true (HTTPS만)
- **SameSite**: None (CORS 요청 허용)

### 6-2. API 요청 시 토큰 포함 방법

#### 방법 A: 자동 포함 (권장)

```javascript
// credentials: 'include'를 사용하면 모든 요청에 자동으로 쿠키가 포함됩니다

const response = await fetch(
  'http://localhost:8080/api/user/profile',
  {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include' // ⭐ 이 옵션이 핵심!
  }
);
```

#### 방법 B: 수동으로 Authorization 헤더에 포함

```javascript
// 만약 credentials 옵션이 작동하지 않으면, 수동으로 헤더에 추가

function getAccessTokenFromCookie() {
  const value = `; ${document.cookie}`;
  const parts = value.split('; access_token_chessmate=');
  if (parts.length === 2) {
    return parts.pop().split(';').shift();
  }
  return null;
}

const accessToken = getAccessTokenFromCookie();

const response = await fetch(
  'http://localhost:8080/api/user/profile',
  {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}` // ⭐ 수동 포함
    },
    credentials: 'include'
  }
);
```

### 6-3. 요청/응답 흐름

```
프론트엔드 요청:
GET /api/user/profile HTTP/1.1
Host: localhost:8080
Cookie: access_token_chessmate=xxxxx; refresh_token_chessmate=yyyyy
Content-Type: application/json

백엔드 처리:
1. 요청의 쿠키에서 access_token_chessmate 추출
2. JWT 서명 검증
3. 토큰에서 사용자 ID 추출
4. 해당 사용자 정보와 함께 요청 처리
5. 응답 반환

프론트엔드 응답:
HTTP/1.1 200 OK
Content-Type: application/json

{
  "code": 200,
  "message": "프로필 조회 성공",
  "data": { ... }
}
```

---

## Step 7: 토큰 갱신 (Refresh)

### 7-1. 토큰 만료 시간

- **Access Token**: 60초 (매우 짧음)
- **Refresh Token**: 30일 (길음)

### 7-2. Access Token 만료 처리

Access Token이 만료되었을 때 (401 Unauthorized 응답):

```javascript
// utils/apiClient.js

async function apiFetch(url, options = {}) {
  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include', // ⭐ 쿠키 자동 포함
    ...options,
  };

  let response = await fetch(url, defaultOptions);

  // 401 Unauthorized 처리
  if (response.status === 401) {
    console.log('Access Token 만료. 토큰 갱신 시도 중...');
    
    // Step 7-3로 진행: 토큰 갱신
    const refreshed = await refreshAccessToken();
    
    if (refreshed) {
      // 갱신 성공 후 원래 요청 재시도
      response = await fetch(url, defaultOptions);
    } else {
      // 갱신 실패: 로그인 페이지로 리다이렉트
      window.location.href = '/login';
      return null;
    }
  }

  return response;
}

export default apiFetch;
```

### 7-3. 토큰 갱신 요청

#### 백엔드 엔드포인트

- **URL**: `GET /api/auth/refresh`
- **인증**: 필요 없음 (Refresh Token이 쿠키에 있으면 됨)
- **Request Body**: 없음
- **내부 동작**:
  1. 요청의 쿠키에서 `refresh_token_chessmate` 추출
  2. Refresh Token 검증 (서명 + Redis 일치 확인)
  3. 새 Access Token 발급
  4. Set-Cookie 헤더로 새 토큰 전송

#### 프론트엔드 구현

```javascript
async function refreshAccessToken() {
  try {
    const response = await fetch(
      'http://localhost:8080/api/auth/refresh',
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include' // ⭐ Refresh Token 쿠키 포함 (필수!)
      }
    );

    if (response.ok) {
      console.log('Access Token 갱신 성공');
      // 새 Access Token이 Set-Cookie로 자동 저장됨
      return true;
    } else {
      console.error('Token 갱신 실패:', response.status);
      return false;
    }

  } catch (error) {
    console.error('Token 갱신 요청 실패:', error);
    return false;
  }
}
```

### 7-4. 자동 갱신 인터셉터 (권장)

```javascript
// utils/interceptor.js

const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true, // 쿠키 자동 포함
});

// 응답 인터셉터: 401 발생 시 자동 갱신
apiClient.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    // 401 에러이고 이미 재시도하지 않은 요청
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // 토큰 갱신
        await refreshAccessToken();
        
        // 원래 요청 재시도
        return apiClient(originalRequest);
      } catch (refreshError) {
        // 갱신 실패: 로그인 페이지로
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## Step 8: 로그아웃

### 8-1. 백엔드 엔드포인트

- **URL**: `POST /api/auth/logout`
- **인증**: 필수 (현재 로그인한 사용자)
- **Request Body**: 없음
- **내부 동작**:
  1. 요청 쿠키의 두 토큰 추출
  2. 쿠키 만료 (maxAge=0)
  3. Redis에서 Refresh Token 삭제
  4. 응답 반환

### 8-2. 프론트엔드 구현

```javascript
// pages/LogoutButton.jsx

import { useNavigate } from 'react-router-dom';

export default function LogoutButton() {
  const navigate = useNavigate();

  async function handleLogout() {
    try {
      // 1단계: 백엔드에 로그아웃 요청
      const response = await fetch(
        'http://localhost:8080/api/auth/logout',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include' // ⭐ 쿠키 포함 (인증용)
        }
      );

      if (!response.ok) {
        throw new Error('로그아웃 실패');
      }

      console.log('로그아웃 성공');

      // 2단계: 로컬 상태 정리
      localStorage.removeItem('user');
      localStorage.removeItem('accessToken');

      // 3단계: 로그인 페이지로 리다이렉트
      navigate('/login');

    } catch (error) {
      console.error('로그아웃 처리 중 오류:', error);
    }
  }

  return (
    <button onClick={handleLogout}>
      로그아웃
    </button>
  );
}
```

### 8-3. 로그아웃 후 쿠키 상태

```javascript
// 백엔드 응답:
HTTP/1.1 200 OK
Set-Cookie: access_token_chessmate=; Path=/; Secure; SameSite=None; Max-Age=0
Set-Cookie: refresh_token_chessmate=; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=0

// 프론트엔드 result:
// 두 쿠키 모두 삭제됨
```

---

## 🍪 쿠키 구조 상세 설명

### 쿠키 비교표

| 항목 | Access Token | Refresh Token |
|------|--------------|---------------|
| **쿠키 이름** | `access_token_chessmate` | `refresh_token_chessmate` |
| **유효 기간** | 60초 (1분) | 30일 |
| **HttpOnly** | ❌ false | ✅ true |
| **Secure** | ✅ true | ✅ true |
| **SameSite** | None | Lax |
| **목적** | API 요청 인증 | 토큰 갱신 |
| **저장 위치** | 메모리 또는 LocalStorage | 브라우저 자동 |
| **프론트엔드 접근** | ✅ 가능 | ❌ 불가능 (XSS 방지) |
| **CORS 요청** | ✅ 자동 포함 | ✅ 자동 포함 |

### 쿠키 설정 예시 (백엔드에서 생성)

```
# Access Token 쿠키
Set-Cookie: access_token_chessmate=eyJhbGciOiJIUzI1NiJ9...; 
            Path=/; 
            HttpOnly=false; 
            Secure=true; 
            SameSite=None; 
            Max-Age=60;

# Refresh Token 쿠키
Set-Cookie: refresh_token_chessmate=eyJhbGciOiJIUzI1NiJ9...; 
            Path=/; 
            HttpOnly=true; 
            Secure=true; 
            SameSite=Lax; 
            Max-Age=2592000;
```

### 로컬 쿠키 확인 방법 (개발자 도구)

```javascript
// 브라우저 콘솔에서 실행
console.log(document.cookie);
// 출력: access_token_chessmate=xxxxx; refresh_token_chessmate=yyyyy

// 특정 쿠키만 조회
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
}

console.log(getCookie('access_token_chessmate'));
console.log(getCookie('refresh_token_chessmate'));
```

---

## ⚠️ 에러 처리

### 일반적인 에러 상황과 대응

#### 1. 401 Unauthorized

```javascript
응답:
HTTP/1.1 401 Unauthorized
{
  "code": 401,
  "message": "JWT 토큰이 유효하지 않음"
}

처리:
- Access Token 갱신 시도
- 갱신 실패 시: 로그인 페이지로 리다이렉트
```

#### 2. 403 Forbidden

```javascript
응답:
HTTP/1.1 403 Forbidden
{
  "code": 403,
  "message": "접근 권한이 없습니다"
}

처리:
- 요청한 리소스에 권한이 없는 경우
- 사용자 역할 확인 필요
```

#### 3. OAuth State 검증 실패

```javascript
응답:
HTTP/1.1 400 Bad Request
{
  "code": 400,
  "message": "Invalid or expired OAuth state"
}

원인:
- state 값이 일치하지 않음
- state가 Redis에서 만료됨 (기본: 10분)
- 다중 탭에서 동시에 로그인 시도

해결:
- 사용자에게 다시 로그인하도록 안내
```

#### 4. Network 에러

```javascript
async function apiRequest(url, options = {}) {
  try {
    const response = await fetch(url, options);
    
    if (!response.ok) {
      // HTTP 에러 처리
      const error = await response.json();
      throw new Error(error.message);
    }

    return await response.json();

  } catch (error) {
    if (error instanceof TypeError) {
      // 네트워크 에러
      console.error('네트워크 연결 실패:', error.message);
      // UI에서 사용자에게 알림
    } else {
      // 기타 에러
      console.error('API 요청 실패:', error.message);
    }
    throw error;
  }
}
```

### 에러 발생 플로우

```
API 요청
  |
  +---> HTTP 에러?
  |       |
  |       +---> 401? ---> Token 갱신 ---> 재시도
  |       |
  |       +---> 403? ---> 권한 에러 처리
  |       |
  |       +---> 기타? ---> 사용자 알림
  |
  +---> 네트워크 에러? ---> 재시도 또는 사용자 알림
```

---

## 🔒 보안 고려사항

### 1. CORS (Cross-Origin Resource Sharing)

```javascript
// 백엔드에서 설정됨 (이미 완료)
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization

// 프론트엔드에서는:
fetch(url, {
  credentials: 'include' // ⭐ 필수!
})
```

### 2. CSRF (Cross-Site Request Forgery) 방지

현재 설정:
- ✅ SameSite 쿠키 속성 사용 (Lax, None)
- ✅ Access Token은 60초로 매우 짧음
- ✅ Refresh Token은 HttpOnly로 보호

### 3. XSS (Cross-Site Scripting) 방지

```javascript
// ❌ 하지 말것:
localStorage.setItem('refreshToken', token); // HttpOnly 쿠키를 쓰세요!

// ✅ 하기:
// Refresh Token은 쿠키에만 저장 (HttpOnly로 보호됨)
// Access Token은 필요시만 쿠키에서 읽기
```

### 4. Token 탈취 방지

```javascript
// ✅ 권장 사항:

// 1. HTTPS만 사용
// Secure: true로 설정되어 있음

// 2. 쿠키 속성 확인
document.cookie; // access_token_chessmate=xxx; refresh_token_chessmate=yyy

// 3. 민감한 정보는 localStorage에 저장 금지
localStorage.removeItem('refreshToken'); // 만약 저장했다면 삭제

// 4. 자동 로그아웃 기능
const INACTIVITY_TIMEOUT = 30 * 60 * 1000; // 30분
let inactivityTimer;

function resetInactivityTimer() {
  clearTimeout(inactivityTimer);
  inactivityTimer = setTimeout(() => {
    logout(); // 30분 이상 활동 없으면 자동 로그아웃
  }, INACTIVITY_TIMEOUT);
}

document.addEventListener('click', resetInactivityTimer);
document.addEventListener('keypress', resetInactivityTimer);
```

### 5. 개발 중 HTTPS 우회 (테스트용만)

```javascript
// .env.development (개발 환경에만)
VITE_API_URL=http://localhost:8080

// 쿠키 Secure 속성을 localhost에서는 작동하지 않으므로
// 백엔드에서 개발 모드 시 Secure=false로 설정해야 함
```

---

## 📱 완벽한 인증 플로우 구현 예제

```javascript
// 종합적인 프론트엔드 인증 관리

import axios from 'axios';

// 1. API 클라이언트 설정
const apiClient = axios.create({
  baseURL: process.env.VITE_API_URL,
  withCredentials: true, // ⭐ 쿠키 자동 포함
});

// 2. 응답 인터셉터 (자동 토큰 갱신)
apiClient.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // 토큰 갱신
        const refreshResponse = await axios.get(
          `${process.env.VITE_API_URL}/api/auth/refresh`,
          { withCredentials: true }
        );

        if (refreshResponse.status === 200) {
          // 원래 요청 재시도
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // 갱신 실패: 로그인 페이지로
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

// 3. 인증 서비스
export const authService = {
  // Lichess 로그인
  async startLichessLogin() {
    const response = await apiClient.get('/api/oauth/oauth-url');
    const { oauthUrl } = response.data.data;
    window.location.href = oauthUrl;
  },

  // 사용자 정보 조회
  async getCurrentUser() {
    const response = await apiClient.get('/api/auth/me');
    return response.data.data;
  },

  // 로그아웃
  async logout() {
    try {
      await apiClient.post('/api/auth/logout');
      localStorage.removeItem('user');
      window.location.href = '/login';
    } catch (error) {
      console.error('로그아웃 실패:', error);
    }
  },
};

export default apiClient;
```

---

## ✅ 체크리스트

프론트엔드 구현 시 다음을 확인하세요:

- [ ] "Lichess로 로그인" 버튼 구현
- [ ] `/api/oauth/oauth-url` 엔드포인트로 OAuth URL 요청
- [ ] OAuth URL로 Lichess로 리다이렉트
- [ ] `/oauth/success` 페이지 구현
- [ ] `/api/auth/me`로 사용자 정보 조회
- [ ] 모든 API 요청에 `credentials: 'include'` 추가
- [ ] 401 응답 시 토큰 갱신 로직 구현
- [ ] `/api/auth/refresh`로 토큰 갱신
- [ ] 로그아웃 버튼 구현
- [ ] `/api/auth/logout` 엔드포인트 호출
- [ ] 에러 처리 로직 구현
- [ ] CORS 설정 확인

---

**문서 작성일**: 2026-02-18
**버전**: 1.0
**마지막 수정**: 2026-02-18

