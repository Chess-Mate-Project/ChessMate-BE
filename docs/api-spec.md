# ChessMate API 명세서

> **Base URL** `https://api.chessladder.org`  
> **최종 수정** 2026-04-15

---

## 목차

1. [공통 규격](#1-공통-규격)
2. [인증 (Auth)](#2-인증-auth)
3. [OAuth 로그인](#3-oauth-로그인)
4. [내 정보 (User)](#4-내-정보-user)
5. [타 유저 조회 (Public User)](#5-타-유저-조회-public-user)
6. [내 통계 (Stat)](#6-내-통계-stat)
7. [동기화 상태 (Sync)](#7-동기화-상태-sync)
8. [이미지 (Image)](#8-이미지-image)
9. [구현 방식](#9-구현-방식)

---

## 1. 공통 규격

### 인증 방식

JWT를 **HttpOnly 쿠키**로 전달합니다. 클라이언트가 Authorization 헤더를 직접 설정할 필요 없습니다.

| 쿠키명 | 용도 | 만료 |
|---|---|---|
| `access_token` | API 인증 | 30분 |
| `refresh_token` | 토큰 재발급 | 7일 |

`🔒` 표시 엔드포인트는 유효한 `access_token` 쿠키가 필요합니다.  
`🔓` 표시 엔드포인트는 인증 없이 접근 가능합니다.

### 공통 응답 구조

```json
{
  "success": true,
  "message": "처리 결과 메시지",
  "data": { }
}
```

### 공통 에러 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

### HTTP 상태 코드

| 코드 | 설명 |
|---|---|
| 200 | 성공 |
| 302 | OAuth 리다이렉트 |
| 400 | 잘못된 요청 (파라미터 누락/오류) |
| 401 | 인증 실패 (토큰 없음·만료) |
| 404 | 리소스 없음 |
| 500 | 서버 내부 오류 |

### platform 파라미터

| 값 | 설명 |
|---|---|
| `LICHESS` | Lichess.org |
| `CHESSCOM` | Chess.com |

### timeClass 파라미터

| 값 | 설명 |
|---|---|
| `bullet` | 불릿 (1분 이하) |
| `blitz` | 블리츠 (3~5분) |
| `rapid` | 래피드 (10~15분) |
| `classical` | 클래시컬 (30분+) |

---

## 2. 인증 (Auth)

### GET /api/auth/me 🔒

현재 인증 상태를 확인합니다. 200이면 인증됨, 401이면 미인증.  
프론트엔드가 앱 진입 시 로그인 여부 및 플랫폼을 판단하는 용도로 사용합니다.

**응답 예시**
```json
{
  "success": true,
  "message": "사용자 정보 조회 성공",
  "data": {
    "userId": 1,
    "username": "wjdansrud0922",
    "platform": "LICHESS",
    "profileImageUrl": "https://cdn.chessladder.org/profile/1/profile.jpg",
    "description": "안녕하세요"
  }
}
```

---

### GET /api/auth/refresh 🔓

Refresh Token 쿠키로 Access Token을 재발급합니다.  
성공 시 새 Access Token이 쿠키에 Set-Cookie됩니다.

**응답 예시**
```json
{ "success": true, "message": "토큰 재발급 성공", "data": null }
```

---

### POST /api/auth/logout 🔒

Access / Refresh Token 쿠키를 만료시키고 Redis에서 Refresh Token을 삭제합니다.

**응답 예시**
```json
{ "success": true, "message": "로그아웃 성공", "data": null }
```

---

## 3. OAuth 로그인

### 전체 흐름

```
① 클라이언트 → GET /login/oauth2/{platform}/url  →  authUrl 획득
② 클라이언트 → 브라우저를 authUrl로 리다이렉트
③ 플랫폼 인증 완료 → 콜백 URL로 리다이렉트
④ 서버 → GET /login/oauth2/code/{platform}?code=&state=  처리
⑤ 서버 → JWT 쿠키 Set-Cookie 후 클라이언트 URL로 302 리다이렉트
⑥ 백그라운드 → SyncJob 생성 → Worker가 게임 수집 시작
```

---

### GET /login/oauth2/lichess/url 🔓

Lichess OAuth 로그인 URL 조회.

**응답 예시**
```json
{
  "success": true,
  "message": "Lichess의 OAuthUrl을 제공합니다.",
  "data": { "url": "https://lichess.org/oauth?client_id=...&state=..." }
}
```

---

### GET /login/oauth2/code/lichess 🔓

Lichess OAuth 콜백. 서버가 처리 후 클라이언트 URL로 리다이렉트합니다.

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `code` | N | Authorization Code (Lichess 발급) |
| `state` | N | PKCE state |

**응답**
```
HTTP 302 → {CLIENT_URL}
Set-Cookie: access_token=...; HttpOnly
Set-Cookie: refresh_token=...; HttpOnly
```

---

### GET /login/oauth2/chesscom/url 🔓

Chess.com OAuth 로그인 URL 조회.

**응답 예시**
```json
{
  "success": true,
  "message": "Chess.com의 OAuthUrl을 제공합니다.",
  "data": { "url": "https://oauth.chess.com/authorize?client_id=..." }
}
```

---

### GET /login/oauth2/code/chesscom 🔓

Chess.com OAuth 콜백. Lichess와 동일한 응답.

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `code` | N | Authorization Code |
| `state` | N | PKCE state |

---

## 4. 내 정보 (User)

### GET /api/user/profile 🔒

내 프로필 조회.

**응답 예시**
```json
{
  "success": true,
  "message": "프로필 조회 성공",
  "data": {
    "id": 1,
    "username": "wjdansrud0922",
    "platform": "LICHESS",
    "description": "안녕하세요",
    "profileImageUrl": "https://cdn.chessladder.org/...",
    "bannerImageUrl": "https://cdn.chessladder.org/...",
    "createdAt": "2024-01-15T10:00:00",
    "platformJoinedAt": "2021-03-10T00:00:00"
  }
}
```

---

### GET /api/user/card 🔒

카드 섹션용 간략 정보 (프로필 이미지 / 배너 / 이름 / 플랫폼).

**응답 예시**
```json
{
  "success": true,
  "message": "카드 조회 성공",
  "data": {
    "username": "wjdansrud0922",
    "platform": "LICHESS",
    "profileImageUrl": "https://cdn.chessladder.org/...",
    "bannerImageUrl": "https://cdn.chessladder.org/..."
  }
}
```

---

### PUT /api/user/description 🔒

자기소개 수정.

**요청 Body**
```json
{ "description": "새 자기소개" }
```

**응답 예시**
```json
{ "success": true, "message": "자기소개 수정 성공", "data": null }
```

---

## 5. 타 유저 조회 (Public User)

> `{username}` 은 플랫폼 사용자명입니다.  
> 모든 엔드포인트 `🔒`.

---

### GET /api/users/{keyword} 🔒

username에 keyword가 포함된 유저를 검색합니다. 최대 10명을 반환합니다.  
각 유저의 최고 레이팅 타임클래스 기준 rating이 함께 반환됩니다.

| 경로 파라미터 | 필수 | 설명 |
|---|---|---|
| `keyword` | Y | 검색 키워드 (username 부분 일치, 대소문자 무시) |

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `platform` | Y | `LICHESS` \| `CHESSCOM` |

**응답 예시**
```json
{
  "success": true,
  "message": "유저 검색 성공",
  "data": {
    "users": [
      {
        "id": 5,
        "username": "magnus",
        "platform": "LICHESS",
        "rating": 3200,
        "profileImageUrl": "https://cdn.chessladder.org/profile/5/profile.jpg"
      },
      {
        "id": 12,
        "username": "magnusfan",
        "platform": "LICHESS",
        "rating": 1540,
        "profileImageUrl": "https://cdn.chessladder.org/default/profile.jpg"
      }
    ]
  }
}
```

> **참고**: 스탯이 없는 유저(게임 수집 미완료)는 결과에서 제외됩니다.

---

### GET /api/users/{username}/profile 🔒

| 경로 파라미터 | 필수 | 설명 |
|---|---|---|
| `username` | Y | 플랫폼 사용자명 (정확히 일치) |

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `platform` | Y | `LICHESS` \| `CHESSCOM` |

**응답 예시**
```json
{
  "success": true,
  "message": "프로필 조회 성공",
  "data": {
    "id": 5,
    "username": "magnus",
    "platform": "LICHESS",
    "description": "World Champion",
    "profileImageUrl": "https://cdn.chessladder.org/...",
    "bannerImageUrl": "https://cdn.chessladder.org/...",
    "createdAt": "2023-05-01T00:00:00",
    "platformJoinedAt": "2012-11-10T00:00:00"
  }
}
```

---

### GET /api/users/{username}/stats/perf 🔒

타임클래스별 레이팅 + 승/무/패 통계.

| 경로 파라미터 | 필수 | 설명 |
|---|---|---|
| `username` | Y | 플랫폼 사용자명 |

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `platform` | Y | `LICHESS` \| `CHESSCOM` |
| `timeClass` | N | 특정 타임클래스만 조회 |

**응답 예시**
```json
{
  "success": true,
  "message": "퍼프 통계 조회 성공",
  "data": [
    { "timeClass": "blitz",  "rating": 1850, "games": 436, "wins": 210, "draws": 30, "losses": 196 },
    { "timeClass": "rapid",  "rating": 1920, "games": 120, "wins": 65,  "draws": 10, "losses": 45  },
    { "timeClass": "bullet", "rating": 1780, "games": 88,  "wins": 44,  "draws": 5,  "losses": 39  }
  ]
}
```

---

### GET /api/users/{username}/stats/streak 🔒

일별 게임 스트릭.

| 경로 파라미터 | 필수 | 설명 |
|---|---|---|
| `username` | Y | 플랫폼 사용자명 |

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `platform` | Y | `LICHESS` \| `CHESSCOM` |
| `year` | N | 연도 필터. 없으면 전체 연도 반환 |

**응답 예시**
```json
{
  "success": true,
  "message": "게임 스트릭 조회 성공",
  "data": {
    "currentStreak": 5,
    "years": [
      {
        "year": 2025,
        "days": [
          { "date": "2025-01-01", "total": 3, "wins": 2, "draws": 0, "losses": 1 },
          { "date": "2025-01-02", "total": 5, "wins": 3, "draws": 1, "losses": 1 }
        ]
      }
    ]
  }
}
```

---

### GET /api/users/{username}/stats/color 🔒

색상(WHITE / BLACK)별 승/무/패 통계.

| 경로 파라미터 | 필수 | 설명 |
|---|---|---|
| `username` | Y | 플랫폼 사용자명 |

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `platform` | Y | `LICHESS` \| `CHESSCOM` |
| `timeClass` | N | 타임클래스 필터 |

**응답 예시**
```json
{
  "success": true,
  "message": "색상별 통계 조회 성공",
  "data": [
    { "timeClass": "blitz", "color": "WHITE", "wins": 110, "draws": 15, "losses": 93 },
    { "timeClass": "blitz", "color": "BLACK", "wins": 100, "draws": 15, "losses": 103 }
  ]
}
```

---

### GET /api/users/{username}/stats/first-move 🔒

타임클래스 × 색상별 첫 수 빈도 통계.

| 경로 파라미터 | 필수 | 설명 |
|---|---|---|
| `username` | Y | 플랫폼 사용자명 |

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `platform` | Y | `LICHESS` \| `CHESSCOM` |
| `timeClass` | N | 타임클래스 필터 |

**응답 예시**
```json
{
  "success": true,
  "message": "첫 수 통계 조회 성공",
  "data": [
    { "timeClass": "blitz", "color": "WHITE", "move": "e4", "count": 180 },
    { "timeClass": "blitz", "color": "WHITE", "move": "d4", "count": 42  },
    { "timeClass": "blitz", "color": "BLACK", "move": "e5", "count": 155 }
  ]
}
```

---

### GET /api/users/{username}/stats/rating-history 🔒

최근 12개월 월별 마지막 레이팅 변화.

| 경로 파라미터 | 필수 | 설명 |
|---|---|---|
| `username` | Y | 플랫폼 사용자명 |

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `platform` | Y | `LICHESS` \| `CHESSCOM` |
| `timeClass` | N | 타임클래스 필터. 없으면 전체 타임클래스 |

**응답 예시**
```json
{
  "success": true,
  "message": "레이팅 히스토리 조회 성공",
  "data": {
    "from": "2025-04",
    "to": "2026-04",
    "entries": [
      { "yearMonth": "2025-04", "timeClass": "blitz", "rating": 1820 },
      { "yearMonth": "2025-05", "timeClass": "blitz", "rating": 1835 },
      { "yearMonth": "2026-04", "timeClass": "blitz", "rating": 1850 }
    ]
  }
}
```

---

## 6. 내 통계 (Stat)

> 본인 통계 조회용입니다. JWT에서 userId를 추출하므로 경로에 userId가 없습니다.  
> 타인 통계는 [5. 타 유저 조회](#5-타-유저-조회-public-user)를 사용하세요.  
> 응답 구조는 5번과 동일합니다.

| 엔드포인트 | 파라미터 |
|---|---|
| `GET /api/stat/perf 🔒` | `platform` (필수), `timeClass` (선택) |
| `GET /api/stat/streak 🔒` | `platform` (필수), `year` (선택) |
| `GET /api/stat/color 🔒` | `platform` (필수), `timeClass` (선택) |
| `GET /api/stat/first-move 🔒` | `platform` (필수), `timeClass` (선택) |
| `GET /api/stat/rating-history 🔒` | `platform` (필수), `timeClass` (선택) |

---

## 7. 동기화 상태 (Sync)

### GET /api/sync/status/{userId} 🔒

게임 수집 진행 상태 조회. 프론트엔드 폴링용.

| 쿼리 파라미터 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `platform` | N | `LICHESS` | `LICHESS` \| `CHESSCOM` |

**status 값**

| 값 | 설명 |
|---|---|
| `PENDING` | 큐 대기 중 |
| `RUNNING` | 수집 진행 중 |
| `COMPLETED` | 수집 완료 |
| `FAILED` | 수집 실패 (`errorMsg` 참고) |
| `TOKEN_EXPIRED` | 플랫폼 토큰 만료 → 재로그인 필요 |

**응답 예시**
```json
{
  "success": true,
  "message": "동기화 상태 조회 성공",
  "data": {
    "jobId": 42,
    "status": "RUNNING",
    "totalFetched": 250,
    "syncCursor": "2025/03",
    "errorMsg": null
  }
}
```

**폴링 권장 로직**
```
2초 간격으로 폴링:
  COMPLETED        → 통계 API 호출 가능. 폴링 종료.
  FAILED           → 에러 메시지 표시. 폴링 종료.
  TOKEN_EXPIRED    → 재로그인 유도. 폴링 종료.
  PENDING·RUNNING  → 진행 중 UI 유지.
```

---

## 8. 이미지 (Image)

이미지 업로드는 **Presigned URL** 방식으로 진행합니다.

```
① GET /api/image/upload-url  →  Presigned URL 발급
② 클라이언트 → Presigned URL로 R2에 직접 PUT
③ POST /api/image/upload-complete  →  DB에 이미지 key 저장
```

---

### GET /api/image/upload-url 🔒

Cloudflare R2 Presigned Upload URL 발급.

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `type` | Y | `PROFILE` \| `BANNER` |
| `contentType` | Y | MIME 타입 (예: `image/jpeg`, `image/png`) |

**응답 예시**
```json
{
  "success": true,
  "message": "이미지 업로드 URL 생성 성공",
  "data": { "url": "https://r2.chessladder.org/upload/...?X-Amz-Signature=..." }
}
```

---

### POST /api/image/upload-complete 🔒

R2 업로드 완료 후 호출. DB에 이미지 key가 저장됩니다.

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `type` | Y | `PROFILE` \| `BANNER` |

**응답 예시**
```json
{ "success": true, "message": "업로드 완료", "data": null }
```

---

### GET /api/image/url 🔒

현재 프로필 또는 배너 이미지 URL 조회. key 없으면 기본 이미지 URL 반환.

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `type` | Y | `PROFILE` \| `BANNER` |

**응답 예시**
```json
{
  "success": true,
  "message": "이미지 URL 조회 성공",
  "data": { "url": "https://cdn.chessladder.org/profile/1/profile.jpg" }
}
```

---

## 9. 구현 방식

### 인증 흐름

```
요청
  → JwtAuthenticationFilter
    → HttpOnly 쿠키에서 access_token 추출
    → JwtService.validate()
    → SecurityContextHolder에 UserPrincipal 등록 (userId + platform 포함)
  → Controller
    → @AuthenticationPrincipal UserPrincipal 으로 userId / platform 획득
    → Service 위임
```

JWT 페이로드에 `userId`와 `platform`이 포함되어 있어 매 요청마다 DB 조회 없이 사용자를 식별합니다.

---

### OAuth 로그인 + 게임 수집 연동

```
콜백 처리 (LichessOAuthService / ChesscomOAuthService)
  1. Redis에서 PKCE code_verifier 조회
  2. 플랫폼에 Authorization Code → Access Token 교환
  3. 플랫폼 API로 사용자 정보 조회
  4. lichess_user / chesscom_user 테이블 upsert
  5. JWT 발급 → HttpOnly 쿠키 Set-Cookie
  6. SyncJob 생성 → Redis 큐 enqueue
  7. 클라이언트 URL로 302 리다이렉트
```

---

### 게임 수집 파이프라인

```
ScheduledSyncTrigger  (30분 cron: "0 0/30 * * * *")
  → 전체 사용자 조회 → SyncJob 생성 → Redis 큐 enqueue

SyncJobDispatcher  (1초 polling)
  → Redis 큐 dequeue → Worker 호출

LichessGameSyncWorker
  - DB에 게임 없음 → 전체 수집 (역방향 커서 페이징)
  - DB에 게임 있음 → 증분 수집 (since = max(played_at) + 1ms)

ChessComGameSyncWorker
  - cursor=null     → 전체 아카이브 수집
  - cursor=전월     → 당월 아카이브만 수집 (증분)
  - Access Token 있음 → 병렬 처리 (parallelism=5)
  - Access Token 없음 → 순차 처리

수집 완료 후
  → GameStatAggregator.aggregate()  → stat 테이블 재집계
  → PerfStatFetcher.fetch()         → user_perf_stat 갱신
```

---

### 타 유저 조회 (`/api/users/{userId}`) 구현

기존 `UserService`, `StatService`의 메서드는 이미 `userId`를 직접 파라미터로 받는 구조입니다.  
`PublicUserController`를 추가하여 `@PathVariable Long userId`를 각 서비스에 그대로 위임합니다.  
**서비스 / 도메인 레이어 변경 없이 컨트롤러 한 파일 추가만으로 구현되었습니다.**

```
GET /api/users/5/stats/perf?platform=LICHESS
  → PublicUserController.getPerfStats(5L, LICHESS, null)
  → StatService.getPerfStats(5L, LICHESS, null)   ← 기존 메서드 그대로 재사용
```

---

### 통계 집계 방식 (full recompute)

게임이 추가될 때마다 해당 플랫폼 전체 게임을 메모리에 올려 재집계합니다.

```
GameStatAggregator.aggregate(userId, platform)
  1. game 테이블에서 전체 게임 로드 (rated 필터)
  2. user_daily_game_stat : 날짜별 총 / 승 / 무 / 패
  3. user_color_stat      : (timeClass × 색상)별 승 / 무 / 패
  4. user_first_move_stat : (timeClass × 색상 × 첫수)별 count
  → 각 테이블 @Modifying 벌크 DELETE → saveAll
```

증분 집계 대신 full recompute를 선택한 이유:  
게임 수정·삭제 시에도 항상 데이터 정합성이 보장되며, 현재 사용자 규모에서 성능 문제가 없습니다.