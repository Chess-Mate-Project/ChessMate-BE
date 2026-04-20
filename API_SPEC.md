# ChessMate-BE API 명세서

> 프론트엔드 연동용 전체 REST API 명세서 (더미 데이터 포함)
> Base URL: `http://localhost:8080` (로컬) / `https://chessladder.org` (프로덕션)

---

## 목차

1. [공통 사항](#공통-사항)
2. [공통 Enum](#공통-enum)
3. [인증 API](#-인증-api)
4. [OAuth2 API](#-oauth2-api)
5. [사용자 API](#-사용자-api)
6. [공개 사용자 API](#-공개-사용자-api-인증-불필요)
7. [통계 API](#-통계-api)
8. [랭킹 API](#-랭킹-api)
9. [이미지 API](#️-이미지-api)
10. [동기화 API](#-동기화-api)
11. [API 흐름 예시](#-api-흐름-예시)

---

## 공통 사항

### 응답 포맷

모든 성공 응답은 아래 구조를 따릅니다:

```json
{
  "success": true,
  "message": "작업 결과 메시지",
  "data": { }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| success | boolean | 항상 `true` |
| message | String | 결과 메시지 (한글) |
| data | T | 실제 응답 데이터 |

### 인증 방식

- **JWT 기반 쿠키 인증** (localStorage 아님)
- `accessToken`, `refreshToken` 쿠키가 자동으로 포함됨 (`httpOnly`, `Secure`)
- 요청 시 별도 헤더 없이 쿠키 자동 전송 (`credentials: include` 설정 필요)

### CORS 설정

| 항목 | 값 |
|------|-----|
| 허용 도메인 | `http://localhost:5173`, `http://localhost:5174`, `https://chessladder.org`, `https://www.chessladder.org` |
| 허용 메서드 | GET, POST, PUT, DELETE, PATCH, OPTIONS |
| Credentials | `true` |

### 인증 필요 여부 요약

| 경로 | 인증 필수 |
|------|---------|
| `/api/auth/**` | ❌ (logout, refresh는 특별 처리) |
| `/login/oauth2/**` | ❌ |
| `/api/rank/ranking` | ❌ |
| `/api/user/platform-stats` | ❌ |
| `/api/users/{username}/**` | ❌ |
| `/api/user/**` | ✅ |
| `/api/stat/**` | ✅ |
| `/api/image/**` | ✅ |
| `/api/sync/**` | ✅ |

---

## 공통 Enum

### OAuthPlatForm
```
CHESSCOM | LICHESS
```

### GameType
```
RAPID | BLITZ | CLASSICAL | BULLET
```

### SyncStatus
```
PENDING | IN_PROGRESS | COMPLETED | FAILED | TOKEN_EXPIRED
```

### UserImageType
```
PROFILE | BANNER
```

---

## 🔑 인증 API

### GET /api/auth/me — 현재 로그인 사용자 정보 조회

인증: ✅ 필수

**응답 예시**
```json
{
  "success": true,
  "message": "사용자 정보 조회 성공",
  "data": {
    "userId": 1,
    "username": "magnuscarlsen",
    "platform": "LICHESS",
    "profileImageUrl": "https://cdn.chessladder.org/profile/magnuscarlsen.jpg",
    "description": "World Chess Champion"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| username | String | 체스 플랫폼 username |
| platform | OAuthPlatForm | `LICHESS` 또는 `CHESSCOM` |
| profileImageUrl | String | 프로필 이미지 URL |
| description | String | 자기소개 |

**에러**
- `401` — 미인증 (쿠키 없음 또는 만료)

---

### POST /api/auth/refresh — 토큰 재발급

인증: ❌ (쿠키의 refreshToken 사용)

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| provider | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |

**요청 예시**
```
POST /api/auth/refresh?provider=LICHESS
```

**응답 예시**
```json
{
  "success": true,
  "message": "토큰 재발급 성공",
  "data": null
}
```

> 응답 쿠키에 새로운 `accessToken` 및 `refreshToken` 자동 설정됨

---

### POST /api/auth/logout — 로그아웃

인증: ✅ 필수

**응답 예시**
```json
{
  "success": true,
  "message": "로그아웃 성공",
  "data": null
}
```

---

## 🔓 OAuth2 API

### GET /login/oauth2/chesscom/url — Chess.com OAuth URL 조회

인증: ❌

**응답 예시**
```json
{
  "success": true,
  "message": "Chess.com의 OAuthUrl을 제공합니다.",
  "data": {
    "platform": "CHESSCOM",
    "oauthUrl": "https://www.chess.com/auth/oauth/authorize?client_id=abc&redirect_uri=..."
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| platform | OAuthPlatForm | `CHESSCOM` |
| oauthUrl | String | Chess.com OAuth 인증 페이지 URL |

---

### GET /login/oauth2/code/chesscom — Chess.com OAuth 콜백

인증: ❌

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | String | ❌ | OAuth 인증 코드 |
| state | String | ❌ | CSRF 토큰 |

**응답**
- `302 Redirect` → 클라이언트 도메인
- 응답 쿠키: `accessToken`, `refreshToken` 설정

---

### GET /login/oauth2/lichess/url — Lichess OAuth URL 조회

인증: ❌

**응답 예시**
```json
{
  "success": true,
  "message": "Lichess의 OAuthUrl을 제공합니다.",
  "data": {
    "platform": "LICHESS",
    "oauthUrl": "https://lichess.org/oauth?client_id=xyz&redirect_uri=..."
  }
}
```

---

### GET /login/oauth2/code/lichess — Lichess OAuth 콜백

인증: ❌

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | String | ❌ | OAuth 인증 코드 |
| state | String | ❌ | CSRF 토큰 |

**응답**
- `302 Redirect` → 클라이언트 도메인
- 응답 쿠키: `accessToken`, `refreshToken` 설정

---

## 👤 사용자 API

### GET /api/user/profile — 내 프로필 조회

인증: ✅ 필수

**응답 예시**
```json
{
  "success": true,
  "message": "프로필 조회 성공",
  "data": {
    "id": 1,
    "username": "magnuscarlsen",
    "platform": "LICHESS",
    "description": "I love chess!",
    "profileImageUrl": "https://cdn.chessladder.org/profile/magnuscarlsen.jpg",
    "bannerImageUrl": "https://cdn.chessladder.org/banner/magnuscarlsen.jpg",
    "createdAt": "2024-01-15T10:30:00",
    "platformJoinedAt": "2020-06-20T12:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 사용자 ID |
| username | String | 체스 플랫폼 username |
| platform | OAuthPlatForm | `LICHESS` 또는 `CHESSCOM` |
| description | String | 자기소개 |
| profileImageUrl | String | 프로필 이미지 URL |
| bannerImageUrl | String | 배너 이미지 URL |
| createdAt | LocalDateTime | ChessMate 가입일 |
| platformJoinedAt | LocalDateTime | 체스 플랫폼 가입일 (스트릭 기준) |

---

### GET /api/user/card — 내 카드 정보 조회

인증: ✅ 필수

**응답 예시**
```json
{
  "success": true,
  "message": "카드 조회 성공",
  "data": {
    "username": "magnuscarlsen",
    "platform": "LICHESS",
    "profileImageUrl": "https://cdn.chessladder.org/profile/magnuscarlsen.jpg",
    "bannerImageUrl": "https://cdn.chessladder.org/banner/magnuscarlsen.jpg"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| username | String | 체스 username |
| platform | OAuthPlatForm | `LICHESS` 또는 `CHESSCOM` |
| profileImageUrl | String | 프로필 이미지 URL |
| bannerImageUrl | String | 배너 이미지 URL |

---

### PUT /api/user/description — 자기소개 수정

인증: ✅ 필수

**요청 바디**
```json
{
  "description": "새로운 자기소개 텍스트입니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| description | String | ✅ | 새 자기소개 |

**응답 예시**
```json
{
  "success": true,
  "message": "자기소개 수정 성공",
  "data": null
}
```

---

### DELETE /api/user — 회원 탈퇴

인증: ✅ 필수

**응답 예시**
```json
{
  "success": true,
  "message": "회원 탈퇴 성공",
  "data": null
}
```

> 계정 삭제 및 인증 쿠키 제거 처리

---

### GET /api/user/platform-stats — 플랫폼별 사용자 수 조회

인증: ❌

**응답 예시**
```json
{
  "success": true,
  "message": "플랫폼별 유저 수 조회 성공",
  "data": {
    "lichessCount": 150,
    "chesscomCount": 120,
    "totalCount": 270
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| lichessCount | int | Lichess 연동 사용자 수 |
| chesscomCount | int | Chess.com 연동 사용자 수 |
| totalCount | int | 전체 사용자 수 |

---

## 🔍 공개 사용자 API (인증 불필요)

### GET /api/users/{username} — 사용자 검색

인증: ❌

**경로 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| username | String | 검색 키워드 (부분 일치) |

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |

**요청 예시**
```
GET /api/users/magnus?platform=LICHESS
```

**응답 예시**
```json
{
  "success": true,
  "message": "유저 검색 성공",
  "data": {
    "users": [
      {
        "id": 1,
        "rating": 2850,
        "profileImageUrl": "https://cdn.chessladder.org/profile/magnuscarlsen.jpg",
        "username": "magnuscarlsen",
        "platform": "LICHESS"
      },
      {
        "id": 2,
        "rating": 2200,
        "profileImageUrl": null,
        "username": "magnus_fan",
        "platform": "LICHESS"
      }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| users | List\<UserSearchProfileResponse\> | 검색 결과 목록 |

**UserSearchProfileResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 사용자 ID |
| rating | int | 현재 레이팅 |
| profileImageUrl | String | 프로필 이미지 URL (없으면 null) |
| username | String | 체스 username |
| platform | OAuthPlatForm | `LICHESS` 또는 `CHESSCOM` |

---

### GET /api/users/{username}/profile — 타인 프로필 조회

인증: ❌

**경로 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| username | String | 체스 플랫폼 username |

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |

**요청 예시**
```
GET /api/users/magnuscarlsen/profile?platform=LICHESS
```

**응답 예시**
```json
{
  "success": true,
  "message": "프로필 조회 성공",
  "data": {
    "id": 1,
    "username": "magnuscarlsen",
    "platform": "LICHESS",
    "description": "World Chess Champion",
    "profileImageUrl": "https://cdn.chessladder.org/profile/magnuscarlsen.jpg",
    "bannerImageUrl": "https://cdn.chessladder.org/banner/magnuscarlsen.jpg",
    "createdAt": "2024-01-15T10:30:00",
    "platformJoinedAt": "2020-06-20T12:00:00"
  }
}
```

---

### GET /api/users/{username}/stats/perf — 타인 레이팅 & 승/무/패 통계

인증: ❌

**경로 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| username | String | 체스 username |

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| timeClass | String | ❌ | `blitz`, `rapid`, `classical`, `bullet` |

**요청 예시**
```
GET /api/users/magnuscarlsen/stats/perf?platform=LICHESS&timeClass=blitz
```

**응답 예시**
```json
{
  "success": true,
  "message": "퍼프 통계 조회 성공",
  "data": [
    {
      "timeClass": "blitz",
      "rating": 2850,
      "games": 1200,
      "wins": 750,
      "losses": 350,
      "draws": 100
    },
    {
      "timeClass": "rapid",
      "rating": 2780,
      "games": 400,
      "wins": 250,
      "losses": 100,
      "draws": 50
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| timeClass | String | 게임 시간 분류 |
| rating | int | 현재 레이팅 |
| games | int | 총 게임 수 |
| wins | int | 승리 수 |
| losses | int | 패배 수 |
| draws | int | 무승부 수 |

---

### GET /api/users/{username}/stats/streak — 타인 게임 스트릭

인증: ❌

**경로 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| username | String | 체스 username |

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| year | Integer | ❌ | 특정 연도 조회 (기본값: 전체) |

**요청 예시**
```
GET /api/users/magnuscarlsen/stats/streak?platform=LICHESS&year=2024
```

**응답 예시**
```json
{
  "success": true,
  "message": "게임 스트릭 조회 성공",
  "data": {
    "currentStreak": 15,
    "years": [
      {
        "year": 2024,
        "days": [
          {
            "date": "2024-01-01",
            "total": 5,
            "wins": 3,
            "draws": 1,
            "losses": 1
          },
          {
            "date": "2024-01-02",
            "total": 3,
            "wins": 2,
            "draws": 0,
            "losses": 1
          }
        ]
      }
    ]
  }
}
```

**StreakResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| currentStreak | int | 현재 연속 게임 일수 |
| years | List\<YearlyGameStatResponse\> | 연도별 일일 통계 |

**YearlyGameStatResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| year | int | 연도 |
| days | List\<DailyGameStatResponse\> | 일일 게임 통계 |

**DailyGameStatResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| date | LocalDate | 날짜 (yyyy-MM-dd) |
| total | int | 총 게임 수 |
| wins | int | 승리 수 |
| draws | int | 무승부 수 |
| losses | int | 패배 수 |

---

### GET /api/users/{username}/stats/color — 타인 색상별 통계

인증: ❌

**경로 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| username | String | 체스 username |

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| timeClass | String | ❌ | 게임 시간 분류 |

**요청 예시**
```
GET /api/users/magnuscarlsen/stats/color?platform=LICHESS&timeClass=blitz
```

**응답 예시**
```json
{
  "success": true,
  "message": "색상별 통계 조회 성공",
  "data": [
    {
      "timeClass": "blitz",
      "color": "white",
      "total": 620,
      "wins": 400,
      "draws": 55,
      "losses": 165
    },
    {
      "timeClass": "blitz",
      "color": "black",
      "total": 580,
      "wins": 350,
      "draws": 45,
      "losses": 185
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| timeClass | String | 게임 시간 분류 |
| color | String | `"white"` 또는 `"black"` |
| total | int | 해당 색상 총 게임 수 |
| wins | int | 승리 수 |
| draws | int | 무승부 수 |
| losses | int | 패배 수 |

---

### GET /api/users/{username}/stats/first-move — 타인 첫 수 통계

인증: ❌

**경로 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| username | String | 체스 username |

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| timeClass | String | ❌ | 게임 시간 분류 |

**요청 예시**
```
GET /api/users/magnuscarlsen/stats/first-move?platform=LICHESS&timeClass=blitz
```

**응답 예시**
```json
{
  "success": true,
  "message": "첫 수 통계 조회 성공",
  "data": [
    {
      "timeClass": "blitz",
      "color": "white",
      "move": "e4",
      "count": 420
    },
    {
      "timeClass": "blitz",
      "color": "white",
      "move": "d4",
      "count": 180
    },
    {
      "timeClass": "blitz",
      "color": "black",
      "move": "e5",
      "count": 310
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| timeClass | String | 게임 시간 분류 |
| color | String | `"white"` 또는 `"black"` |
| move | String | 첫 수 (예: `e4`, `d4`, `c5`) |
| count | int | 해당 첫 수 사용 횟수 |

---

### GET /api/users/{username}/stats/rating-history — 타인 월별 레이팅 변화

인증: ❌

**경로 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| username | String | 체스 username |

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| timeClass | String | ❌ | 게임 시간 분류 |

**요청 예시**
```
GET /api/users/magnuscarlsen/stats/rating-history?platform=LICHESS&timeClass=blitz
```

**응답 예시**
```json
{
  "success": true,
  "message": "레이팅 히스토리 조회 성공",
  "data": {
    "from": "2025-04",
    "to": "2026-04",
    "data": [
      {
        "yearMonth": "2025-04",
        "timeClass": "blitz",
        "rating": 2750
      },
      {
        "yearMonth": "2025-05",
        "timeClass": "blitz",
        "rating": 2780
      },
      {
        "yearMonth": "2025-06",
        "timeClass": "blitz",
        "rating": 2810
      },
      {
        "yearMonth": "2026-04",
        "timeClass": "blitz",
        "rating": 2850
      }
    ]
  }
}
```

**RatingHistoryResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| from | String | 시작 월 (YYYY-MM, 1년 전) |
| to | String | 종료 월 (YYYY-MM, 이번 달) |
| data | List\<MonthlyRatingEntry\> | 월별 레이팅 데이터 |

**MonthlyRatingEntry**

| 필드 | 타입 | 설명 |
|------|------|------|
| yearMonth | String | 연월 (예: `2025-04`) |
| timeClass | String | 게임 시간 분류 |
| rating | int | 해당 월의 레이팅 |

---

## 📊 통계 API

> 현재 로그인한 사용자 기준. `/api/users/{username}/stats/*`와 응답 구조 동일.

### GET /api/stat/streak — 내 게임 스트릭

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| year | Integer | ❌ | 특정 연도 조회 |

**응답**: [StreakResponse 참조](#get-apiusersusernamestatstreak--타인-게임-스트릭)

---

### GET /api/stat/color — 내 색상별 통계

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| timeClass | String | ❌ | 게임 시간 분류 |

**응답**: [ColorStatResponse 참조](#get-apiusersusernamestatcolor--타인-색상별-통계)

---

### GET /api/stat/first-move — 내 첫 수 통계

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| timeClass | String | ❌ | 게임 시간 분류 |

**응답**: [FirstMoveStatResponse 참조](#get-apiusersusernamestatfirst-move--타인-첫-수-통계)

---

### GET /api/stat/perf — 내 레이팅 & 승/무/패 통계

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| timeClass | String | ❌ | 게임 시간 분류 |

**응답**: [UserPerfStatResponse 참조](#get-apiusersusernamestatperf--타인-레이팅--승무패-통계)

---

### GET /api/stat/rating-history — 내 월별 레이팅 변화 (최근 1년)

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` |
| timeClass | String | ❌ | 게임 시간 분류 |

**응답**: [RatingHistoryResponse 참조](#get-apiusersusernamestatrating-history--타인-월별-레이팅-변화)

---

## 🏆 랭킹 API

### GET /api/rank/ranking — 랭킹 조회

인증: ❌ (로그인 시 내 순위 포함)

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 | 기본값 |
|------|------|------|------|-------|
| gameType | GameType | ❌ | `RAPID`, `BLITZ`, `CLASSICAL`, `BULLET` | `RAPID` |
| platform | OAuthPlatForm | ✅ | `LICHESS` 또는 `CHESSCOM` | - |
| page | int | ❌ | 페이지 번호 (0부터 시작) | `0` |
| size | int | ❌ | 페이지 크기 | `20` |

**요청 예시**
```
GET /api/rank/ranking?platform=LICHESS&gameType=BLITZ&page=0&size=20
```

**응답 예시**
```json
{
  "success": true,
  "message": "Ranking 조회 성공",
  "data": {
    "myRankInfo": {
      "loggedInUser": true,
      "platformMismatch": false,
      "rank": 15,
      "rating": 2100,
      "userId": 1,
      "username": "myusername",
      "banner": "https://cdn.chessladder.org/banner/myusername.jpg",
      "profile": "https://cdn.chessladder.org/profile/myusername.jpg",
      "description": "내 자기소개"
    },
    "ranking": [
      {
        "userId": 10,
        "username": "magnuscarlsen",
        "description": "World Champion",
        "rating": 2850,
        "rank": 1,
        "bannerImage": "https://cdn.chessladder.org/banner/magnuscarlsen.jpg",
        "profileImage": "https://cdn.chessladder.org/profile/magnuscarlsen.jpg"
      },
      {
        "userId": 11,
        "username": "hikaru",
        "description": "Speed chess king",
        "rating": 2800,
        "rank": 2,
        "bannerImage": null,
        "profileImage": "https://cdn.chessladder.org/profile/hikaru.jpg"
      }
    ],
    "totalCount": 270,
    "currentPage": 0,
    "pageSize": 20,
    "totalPages": 14
  }
}
```

**RankingResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| myRankInfo | MyRankInfo | 현재 사용자 순위 정보 (비로그인 시 null 가능) |
| ranking | List\<RankerDto\> | 현재 페이지 랭킹 목록 |
| totalCount | long | 전체 사용자 수 |
| currentPage | int | 현재 페이지 (0부터 시작) |
| pageSize | int | 페이지 크기 |
| totalPages | long | 전체 페이지 수 |

**MyRankInfo**

| 필드 | 타입 | 설명 |
|------|------|------|
| loggedInUser | boolean | 로그인 여부 |
| platformMismatch | boolean | 조회 플랫폼과 사용자 플랫폼 불일치 여부 |
| rank | int | 현재 순위 |
| rating | int | 현재 레이팅 |
| userId | Long | 사용자 ID |
| username | String | username |
| banner | String | 배너 이미지 URL |
| profile | String | 프로필 이미지 URL |
| description | String | 자기소개 |

**RankerDto**

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| username | String | username |
| description | String | 자기소개 |
| rating | int | 레이팅 |
| rank | int | 순위 |
| bannerImage | String | 배너 이미지 URL (없으면 null) |
| profileImage | String | 프로필 이미지 URL (없으면 null) |

---

## 🖼️ 이미지 API

> 이미지 업로드는 클라이언트 → R2 직접 업로드 방식 (Presigned URL)

### GET /api/image/upload-url — Presigned URL 생성

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| type | UserImageType | ✅ | `PROFILE` 또는 `BANNER` |
| contentType | String | ✅ | MIME 타입 (예: `image/jpeg`, `image/png`) |

**요청 예시**
```
GET /api/image/upload-url?type=PROFILE&contentType=image/jpeg
```

**응답 예시**
```json
{
  "success": true,
  "message": "이미지 업로드 URL 생성 성공",
  "data": {
    "uploadUrl": "https://r2.example.com/chessmate/profile/user_1.jpg?X-Amz-Signature=..."
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| uploadUrl | String | Cloudflare R2 Presigned URL (PUT 요청용) |

---

### POST /api/image/upload-complete — 업로드 완료 처리

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| type | UserImageType | ✅ | `PROFILE` 또는 `BANNER` |

**요청 예시**
```
POST /api/image/upload-complete?type=PROFILE
```

**응답 예시**
```json
{
  "success": true,
  "message": "업로드 완료",
  "data": null
}
```

---

### GET /api/image/url — 현재 이미지 URL 조회

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| type | UserImageType | ✅ | `PROFILE` 또는 `BANNER` |

**요청 예시**
```
GET /api/image/url?type=PROFILE
```

**응답 예시**
```json
{
  "success": true,
  "message": "이미지 URL 조회 성공",
  "data": {
    "uploadUrl": "https://cdn.chessladder.org/profile/user_1.jpg"
  }
}
```

> 이미지가 없으면 기본 이미지 URL 반환

---

## 🔄 동기화 API

### GET /api/sync/status — 게임 동기화 상태 조회

인증: ✅ 필수

**Query 파라미터**

| 이름 | 타입 | 필수 | 설명 | 기본값 |
|------|------|------|------|-------|
| platform | OAuthPlatForm | ❌ | `LICHESS` 또는 `CHESSCOM` | `LICHESS` |

**요청 예시**
```
GET /api/sync/status?platform=LICHESS
```

**응답 예시 — 진행 중**
```json
{
  "success": true,
  "message": "동기화 상태 조회 성공",
  "data": {
    "jobId": 42,
    "status": "IN_PROGRESS",
    "totalFetched": 150,
    "syncCursor": "game_abc123",
    "errorMsg": null
  }
}
```

**응답 예시 — 완료**
```json
{
  "success": true,
  "message": "동기화 상태 조회 성공",
  "data": {
    "jobId": 42,
    "status": "COMPLETED",
    "totalFetched": 532,
    "syncCursor": "game_xyz999",
    "errorMsg": null
  }
}
```

**응답 예시 — 실패**
```json
{
  "success": true,
  "message": "동기화 상태 조회 성공",
  "data": {
    "jobId": 42,
    "status": "FAILED",
    "totalFetched": 80,
    "syncCursor": null,
    "errorMsg": "External API timeout"
  }
}
```

**SyncStatusResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| jobId | Long | 동기화 작업 ID |
| status | SyncStatus | 현재 동기화 상태 |
| totalFetched | int | 동기화된 총 게임 수 |
| syncCursor | String | 마지막 동기화 지점 (null 가능) |
| errorMsg | String | 오류 메시지 (실패 시, 그 외 null) |

**SyncStatus 값**

| 값 | 설명 |
|-------|------|
| `PENDING` | 대기 중 |
| `IN_PROGRESS` | 동기화 진행 중 |
| `COMPLETED` | 완료 |
| `FAILED` | 실패 |
| `TOKEN_EXPIRED` | 플랫폼 토큰 만료 |

> 프론트엔드 폴링 권장: `IN_PROGRESS` 상태에서 주기적으로 호출, `COMPLETED` / `FAILED` 시 중단

---

## 📖 API 흐름 예시

### 로그인 플로우

```
1. GET /login/oauth2/lichess/url
   → oauthUrl 받음

2. 사용자를 oauthUrl로 리다이렉트

3. (Lichess 인증 후 자동 콜백)
   GET /login/oauth2/code/lichess?code=...&state=...
   → 클라이언트 도메인으로 302 Redirect
   → 쿠키: accessToken, refreshToken 설정

4. GET /api/auth/me
   → 사용자 정보 확인
```

---

### 타인 프로필 전체 조회

```
GET /api/users/{username}/profile?platform=LICHESS
GET /api/users/{username}/stats/perf?platform=LICHESS
GET /api/users/{username}/stats/color?platform=LICHESS
GET /api/users/{username}/stats/first-move?platform=LICHESS
GET /api/users/{username}/stats/streak?platform=LICHESS
GET /api/users/{username}/stats/rating-history?platform=LICHESS
```

---

### 이미지 업로드 플로우

```
1. GET /api/image/upload-url?type=PROFILE&contentType=image/jpeg
   → uploadUrl (Presigned URL) 받음

2. PUT {uploadUrl}
   Headers: Content-Type: image/jpeg
   Body: 이미지 바이너리
   → 클라이언트가 Cloudflare R2에 직접 업로드

3. POST /api/image/upload-complete?type=PROFILE
   → DB에 이미지 key 저장

4. GET /api/image/url?type=PROFILE
   → 저장된 이미지 URL 확인
```

---

### 동기화 상태 폴링

```javascript
const pollSyncStatus = async () => {
  const res = await fetch('/api/sync/status?platform=LICHESS', {
    credentials: 'include'
  });
  const { data } = await res.json();

  if (data.status === 'IN_PROGRESS' || data.status === 'PENDING') {
    setTimeout(pollSyncStatus, 3000); // 3초마다 폴링
  } else {
    // COMPLETED / FAILED / TOKEN_EXPIRED 처리
  }
};
```