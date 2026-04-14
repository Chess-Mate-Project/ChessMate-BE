# ChessMate API 명세서

> Base URL (API 서버): `http://localhost:8080`  
> 인증 방식: JWT (HttpOnly Cookie)  
> 공통 응답 포맷: `{ "message": "...", "data": { ... } }`

---

## 목차

1. [인증 (Auth)](#1-인증-auth)
2. [OAuth — Lichess](#2-oauth--lichess)
3. [OAuth — Chess.com](#3-oauth--chesscom)
4. [게임 수집 상태 (Sync)](#4-게임-수집-상태-sync)
5. [사용자 (User)](#5-사용자-user) ⚠️ 미구현
6. [통계 (Stat)](#6-통계-stat) ⚠️ 미구현
7. [랭킹 (Rank)](#7-랭킹-rank) ⚠️ 미구현
8. [공통 규격](#8-공통-규격)

> ⚠️ 표시 섹션은 서버 코드가 주석 처리된 상태로 아직 활성화되지 않은 엔드포인트입니다.

---

## 1. 인증 (Auth)

### 1-1. 토큰 갱신

```
GET /api/auth/refresh
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 (Refresh Token 쿠키 자동 전송) |
| 설명 | Refresh Token으로 Access Token 재발급. 응답 쿠키에 새 토큰 설정. |

**Request**
```
Cookie: refresh_token=<refresh_jwt>
```

**Response**
```
HTTP 200
Set-Cookie: access_token=<new_jwt>; HttpOnly; Path=/
```
```json
{ "message": "토큰 갱신 성공", "data": null }
```

**Error**
```json
HTTP 401
{ "message": "리프레시 토큰이 유효하지 않습니다." }
```

---

### 1-2. 로그아웃

```
POST /api/auth/logout
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |
| 설명 | 서버 토큰 무효화 + 클라이언트 쿠키 삭제 |

**Response**
```json
HTTP 200
{ "message": "로그아웃 성공", "data": null }
```

---

## 2. OAuth — Lichess

### 전체 흐름

```
① 프론트 → GET /login/oauth2/lichess/url  → { authUrl }
② 프론트 → 브라우저를 authUrl로 리다이렉트
③ Lichess → 인증 완료 후 콜백 URL로 리다이렉트
④ 서버 → GET /login/oauth2/code/lichess?code=...&state=...  처리
⑤ 서버 → 토큰 쿠키 설정 후 클라이언트 URL로 리다이렉트
⑥ 프론트 → 게임 수집 시작 (자동, 백그라운드)
```

### 2-1. OAuth URL 조회

```
GET /login/oauth2/lichess/url
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |

**Response**
```json
HTTP 200
{
  "message": "Lichess OAuth URL 조회 성공",
  "data": {
    "authUrl": "https://lichess.org/oauth?response_type=code&client_id=...&state=..."
  }
}
```

---

### 2-2. OAuth 콜백 (서버 내부 처리)

```
GET /login/oauth2/code/lichess?code={code}&state={state}
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 (Lichess가 직접 호출) |
| 설명 | 인증 코드로 토큰 발급, 사용자 정보 저장, 게임 수집 큐 등록, 클라이언트로 리다이렉트 |

**Response**
```
HTTP 302 → Location: {CLIENT_URL}
Set-Cookie: access_token=...; refresh_token=...
```

---

## 3. OAuth — Chess.com

Lichess와 동일한 흐름.

### 3-1. OAuth URL 조회

```
GET /login/oauth2/chesscom/url
```

**Response** (Lichess와 동일 구조)
```json
{
  "message": "Chess.com OAuth URL 조회 성공",
  "data": {
    "authUrl": "https://oauth.chess.com/authorize?..."
  }
}
```

---

### 3-2. OAuth 콜백 (서버 내부 처리)

```
GET /login/oauth2/code/chesscom?code={code}&state={state}
```

Lichess와 동일.

---

## 4. 게임 수집 상태 (Sync)

OAuth 완료 후 게임 수집이 백그라운드에서 진행됩니다.  
프론트엔드는 이 엔드포인트를 **1~3초 간격으로 폴링**하여 진행 상태를 표시합니다.

### 4-1. 수집 상태 조회

```
GET /api/sync/status/{userId}?platform=LICHESS
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |
| `userId` | 서비스 내 사용자 ID (Path Variable) |
| `platform` | `LICHESS` 또는 `CHESSCOM` (기본값: LICHESS) |

**Response**
```json
HTTP 200
{
  "message": "동기화 상태 조회 성공",
  "data": {
    "jobId": 42,
    "status": "IN_PROGRESS",
    "totalFetched": 320,
    "syncCursor": "abc12xyz",
    "errorMsg": null
  }
}
```

**status 값 정의**

| status | 설명 |
|--------|------|
| `PENDING` | 수집 대기 중 (큐에 등록됨) |
| `IN_PROGRESS` | 수집 진행 중 |
| `COMPLETED` | 수집 완료 |
| `FAILED` | 수집 실패 (`errorMsg` 참조) |
| `TOKEN_EXPIRED` | 플랫폼 토큰 만료 → 재로그인 필요 |

**폴링 권장 로직**
```
poll every 2s:
  if status == COMPLETED → 통계 API 호출 가능, 폴링 중단
  if status == FAILED || TOKEN_EXPIRED → 에러 UI 표시, 폴링 중단
  else → 진행 중 UI 유지
```

---

## 5. 사용자 (User)

> ⚠️ 현재 주석 처리 상태. 향후 활성화 예정.

### 5-1. 전체 사용자 수 조회

```
GET /api/user/count
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |

**Response**
```json
{
  "message": "총 유저 수 조회 성공",
  "data": {
    "totalCount": 1024
  }
}
```

---

### 5-2. 내 프로필 조회

```
GET /api/user/profile
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |

**Response**
```json
{
  "message": "프로필 조회 성공",
  "data": {
    "userId": 1,
    "username": "hikaru",
    "description": "자기소개",
    "profileImageUrl": "https://...",
    "bannerImageUrl": "https://..."
  }
}
```

---

### 5-3. 자기소개 수정

```
PUT /api/user/description
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |

**Request Body**
```json
{ "description": "새로운 자기소개" }
```

**Response**
```json
{ "message": "자기소개 수정 성공", "data": null }
```

---

### 5-4. 회원 탈퇴

```
DELETE /api/user/withdraw
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |

**Response**
```json
{ "message": "회원 탈퇴 성공", "data": null }
```

---

## 6. 통계 (Stat)

> ⚠️ 현재 주석 처리 상태. 게임 수집 완료 후 조회 가능.  
> `platform` 파라미터는 향후 추가 예정 (현재 Lichess 기준).

### 6-1. 연간 플레이 스트릭

```
GET /api/stat/streak?year=2024
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |
| `year` | 조회 연도 (예: 2024) |

**Response**
```json
{
  "message": "스트릭 조회 성공",
  "data": {
    "year": 2024,
    "days": [
      {
        "date": "2024-01-15",
        "total": 12,
        "wins": 7,
        "draws": 2,
        "losses": 3
      }
    ]
  }
}
```

---

### 6-2. 색상별 승률 통계

```
GET /api/stat/color?gameType=RAPID
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |
| `gameType` | `BULLET` / `BLITZ` / `RAPID` / `CLASSICAL` (기본값: RAPID) |

**Response**
```json
{
  "message": "색상별 통계 조회 성공",
  "data": {
    "gameType": "RAPID",
    "white": {
      "wins": 120,
      "draws": 15,
      "losses": 65,
      "total": 200,
      "winRate": 60.0
    },
    "black": {
      "wins": 95,
      "draws": 20,
      "losses": 85,
      "total": 200,
      "winRate": 47.5
    }
  }
}
```

---

### 6-3. 첫 수 통계

```
GET /api/stat/first-move?gameType=RAPID
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |
| `gameType` | `BULLET` / `BLITZ` / `RAPID` / `CLASSICAL` (기본값: RAPID) |

**Response**
```json
{
  "message": "첫수 통계 조회 성공",
  "data": {
    "gameType": "RAPID",
    "asWhite": [
      { "move": "e4", "count": 312 },
      { "move": "d4", "count": 98 },
      { "move": "Nf3", "count": 45 }
    ],
    "asBlack": [
      { "move": "e5", "count": 280 },
      { "move": "c5", "count": 120 },
      { "move": "e6", "count": 60 }
    ]
  }
}
```

---

### 6-4. 퍼포먼스(레이팅) 조회

```
GET /api/stat/perf?gameType=RAPID
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |

**Response**
```json
{
  "message": "퍼포먼스 조회 성공",
  "data": {
    "gameType": "RAPID",
    "rating": 1854,
    "games": 400,
    "wins": 215,
    "draws": 35,
    "losses": 150
  }
}
```

---

### 6-5. 통계 강제 갱신

```
PUT /api/stat/force-refresh
```

| 항목 | 내용 |
|------|------|
| 인증 | 필수 |
| 설명 | 집계 통계 수동 재계산 트리거 |

**Response**
```json
{
  "message": "통계 갱신 완료",
  "data": {
    "tier": "GOLD"
  }
}
```

---

## 7. 랭킹 (Rank)

> ⚠️ 현재 주석 처리 상태.

### 7-1. 게임 타입별 랭킹 조회

```
GET /api/rank/ranking?gameType=RAPID&page=0&size=20
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 (로그인 시 내 순위 포함) |
| `gameType` | `BULLET` / `BLITZ` / `RAPID` / `CLASSICAL` (기본값: RAPID) |
| `page` | 페이지 번호 (0-based) |
| `size` | 페이지 크기 (기본값: 20) |

**Response**
```json
{
  "message": "랭킹 조회 성공",
  "data": {
    "gameType": "RAPID",
    "totalCount": 1024,
    "myRank": 42,
    "entries": [
      {
        "rank": 1,
        "userId": 7,
        "username": "hikaru",
        "rating": 2850,
        "tier": "MASTER"
      }
    ]
  }
}
```

---

## 8. 공통 규격

### 공통 응답 포맷

```json
{
  "message": "설명 메시지",
  "data": { }
}
```

### 에러 응답 포맷

```json
{
  "message": "에러 메시지",
  "data": null
}
```

### HTTP 상태 코드

| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 302 | OAuth 리다이렉트 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 (토큰 없음/만료) |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 내부 오류 |

### 인증 방식

| 항목 | 내용 |
|------|------|
| 방식 | JWT (HttpOnly Cookie) |
| Access Token Cookie | `access_token` (단기, 약 1시간) |
| Refresh Token Cookie | `refresh_token` (장기, 약 14일) |
| 갱신 방법 | 401 수신 시 `GET /api/auth/refresh` 호출 |

### 클라이언트 OAuth 연동 전체 시퀀스

```
[최초 로그인]
1. GET /login/oauth2/lichess/url → authUrl 획득
2. window.location.href = authUrl (브라우저 리다이렉트)
3. Lichess 인증 완료 → 서버 콜백 → 클라이언트로 리다이렉트
4. 쿠키에 JWT 자동 설정됨

[게임 수집 상태 폴링]
5. userId 파악 (프로필 조회 또는 JWT 파싱)
6. GET /api/sync/status/{userId}?platform=LICHESS 폴링 (2초 간격)
7. status == COMPLETED → 통계 API 호출 가능

[Chess.com 추가 연동]
8. GET /login/oauth2/chesscom/url → authUrl 획득
9. 동일 흐름 반복
10. GET /api/sync/status/{userId}?platform=CHESSCOM 폴링
```

### gameType 값 정의

| 값 | 설명 | 대응 time_class |
|----|------|----------------|
| `BULLET` | 초단기전 (1분 이하) | bullet |
| `BLITZ` | 블리츠 (3~5분) | blitz |
| `RAPID` | 래피드 (10~15분) | rapid |
| `CLASSICAL` | 클래시컬 (30분+) | classical |

### platform 값 정의

| 값 | 설명 |
|----|------|
| `LICHESS` | Lichess.org |
| `CHESSCOM` | Chess.com |