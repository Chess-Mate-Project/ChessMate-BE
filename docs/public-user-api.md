# 타 유저 프로필 / 통계 조회 API

> **Base URL** `https://api.chessladder.org`
> **인증** 로그인 상태 필요 (JWT 쿠키 자동 전송, 별도 설정 불필요)

---

## 한 눈에 보기

| 기능 | 요청 |
|---|---|
| 프로필 | `GET /api/users/{username}/profile?platform=` |
| 레이팅 · 승/무/패 | `GET /api/users/{username}/stats/perf?platform=` |
| 게임 스트릭 | `GET /api/users/{username}/stats/streak?platform=` |
| 색상별 통계 | `GET /api/users/{username}/stats/color?platform=` |
| 첫 수 통계 | `GET /api/users/{username}/stats/first-move?platform=` |
| 월별 레이팅 변화 | `GET /api/users/{username}/stats/rating-history?platform=` |

---

## 공통 사항

### Path Variable

| 변수 | 설명 | 예시 |
|---|---|---|
| `username` | 조회할 유저의 플랫폼 username | `magnus`, `hikaru` |

### Query Parameter

| 파라미터 | 필수 | 값 | 설명 |
|---|---|---|---|
| `platform` | **필수** | `LICHESS` \| `CHESSCOM` | Lichess와 Chess.com은 동일 username이 다른 사람일 수 있어 필수 |
| `timeClass` | 선택 | `bullet` \| `blitz` \| `rapid` \| `classical` | 일부 엔드포인트에서 사용. 미지정 시 전체 반환 |
| `year` | 선택 | 예: `2025` | streak 엔드포인트 전용. 미지정 시 전체 연도 반환 |

### 공통 응답 구조

```json
{
  "success": true,
  "message": "...",
  "data": { }
}
```

### 에러 응답

| 상황 | status | message |
|---|---|---|
| 해당 username이 ChessMate에 없음 | `404` | `"사용자를 찾을 수 없습니다."` |
| 로그인 안 됨 / 토큰 만료 | `401` | — |
| platform 파라미터 누락 | `400` | — |

---

## GET /api/users/{username}/profile

유저의 기본 프로필 정보를 반환합니다.

**요청 예시**
```
GET /api/users/magnus/profile?platform=LICHESS
```

**응답**
```json
{
  "success": true,
  "message": "프로필 조회 성공",
  "data": {
    "id": 5,
    "username": "magnus",
    "platform": "LICHESS",
    "description": "체스를 좋아합니다",
    "profileImageUrl": "https://cdn.chessladder.org/profile/5/profile.jpg",
    "bannerImageUrl": "https://cdn.chessladder.org/banner/5/banner.jpg",
    "createdAt": "2023-05-01T00:00:00",
    "platformJoinedAt": "2012-11-10T00:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | number | 서비스 내부 사용자 ID |
| `username` | string | 플랫폼 username |
| `platform` | string | `LICHESS` \| `CHESSCOM` |
| `description` | string \| null | 자기소개 |
| `profileImageUrl` | string | 프로필 이미지 URL (없으면 기본 이미지) |
| `bannerImageUrl` | string | 배너 이미지 URL (없으면 기본 이미지) |
| `createdAt` | string | ChessMate 가입일 (ISO 8601) |
| `platformJoinedAt` | string \| null | 플랫폼(Lichess/Chess.com) 계정 생성일 |

---

## GET /api/users/{username}/stats/perf

타임클래스별 레이팅, 총 게임 수, 승/무/패를 반환합니다.

**요청 예시**
```
GET /api/users/magnus/stats/perf?platform=LICHESS
GET /api/users/magnus/stats/perf?platform=LICHESS&timeClass=blitz
```

**응답**
```json
{
  "success": true,
  "message": "퍼프 통계 조회 성공",
  "data": [
    {
      "timeClass": "bullet",
      "rating": 1780,
      "games": 88,
      "wins": 44,
      "draws": 5,
      "losses": 39
    },
    {
      "timeClass": "blitz",
      "rating": 1850,
      "games": 436,
      "wins": 210,
      "draws": 30,
      "losses": 196
    },
    {
      "timeClass": "rapid",
      "rating": 1920,
      "games": 120,
      "wins": 65,
      "draws": 10,
      "losses": 45
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `timeClass` | string | `bullet` \| `blitz` \| `rapid` \| `classical` |
| `rating` | number | 현재 레이팅 |
| `games` | number | 총 게임 수 |
| `wins` / `draws` / `losses` | number | 승 / 무 / 패 |

---

## GET /api/users/{username}/stats/streak

날짜별 게임 플레이 기록과 현재 연속 플레이 일수를 반환합니다.

**요청 예시**
```
GET /api/users/magnus/stats/streak?platform=LICHESS
GET /api/users/magnus/stats/streak?platform=LICHESS&year=2025
```

**응답**
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

| 필드 | 타입 | 설명 |
|---|---|---|
| `currentStreak` | number | 오늘 또는 어제 기준 연속 플레이 일수 |
| `years` | array | 연도별 그룹 |
| `years[].year` | number | 연도 |
| `years[].days` | array | 게임을 플레이한 날짜 목록 (플레이 없는 날은 포함되지 않음) |
| `days[].date` | string | 날짜 (`yyyy-MM-dd`) |
| `days[].total` | number | 해당 날 총 게임 수 |
| `days[].wins` / `draws` / `losses` | number | 승 / 무 / 패 |

---

## GET /api/users/{username}/stats/color

색상(WHITE / BLACK)별 승/무/패 통계를 반환합니다.

**요청 예시**
```
GET /api/users/magnus/stats/color?platform=LICHESS
GET /api/users/magnus/stats/color?platform=LICHESS&timeClass=blitz
```

**응답**
```json
{
  "success": true,
  "message": "색상별 통계 조회 성공",
  "data": [
    { "timeClass": "blitz", "color": "WHITE", "wins": 110, "draws": 15, "losses": 93 },
    { "timeClass": "blitz", "color": "BLACK", "wins": 100, "draws": 15, "losses": 103 },
    { "timeClass": "rapid", "color": "WHITE", "wins": 35,  "draws": 5,  "losses": 20  },
    { "timeClass": "rapid", "color": "BLACK", "wins": 30,  "draws": 5,  "losses": 25  }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `timeClass` | string | 타임클래스 |
| `color` | string | `WHITE` \| `BLACK` |
| `wins` / `draws` / `losses` | number | 승 / 무 / 패 |

---

## GET /api/users/{username}/stats/first-move

타임클래스 × 색상별 첫 수 빈도를 반환합니다.

**요청 예시**
```
GET /api/users/magnus/stats/first-move?platform=LICHESS
GET /api/users/magnus/stats/first-move?platform=LICHESS&timeClass=blitz
```

**응답**
```json
{
  "success": true,
  "message": "첫 수 통계 조회 성공",
  "data": [
    { "timeClass": "blitz", "color": "WHITE", "move": "e4", "count": 180 },
    { "timeClass": "blitz", "color": "WHITE", "move": "d4", "count": 42  },
    { "timeClass": "blitz", "color": "BLACK", "move": "e5", "count": 155 },
    { "timeClass": "blitz", "color": "BLACK", "move": "c5", "count": 88  }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `timeClass` | string | 타임클래스 |
| `color` | string | `WHITE` \| `BLACK` |
| `move` | string | 첫 번째 수 (예: `e4`, `d4`, `Nf3`) |
| `count` | number | 해당 수를 선택한 게임 수 |

---

## GET /api/users/{username}/stats/rating-history

최근 12개월 월별 마지막 레이팅 변화를 반환합니다.

**요청 예시**
```
GET /api/users/magnus/stats/rating-history?platform=LICHESS
GET /api/users/magnus/stats/rating-history?platform=LICHESS&timeClass=blitz
```

**응답**
```json
{
  "success": true,
  "message": "레이팅 히스토리 조회 성공",
  "data": {
    "from": "2025-04",
    "to": "2026-04",
    "entries": [
      { "yearMonth": "2025-04", "timeClass": "blitz", "rating": 1820 },
      { "yearMonth": "2025-06", "timeClass": "blitz", "rating": 1835 },
      { "yearMonth": "2026-04", "timeClass": "blitz", "rating": 1850 }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `from` | string | 조회 시작 월 (`yyyy-MM`) |
| `to` | string | 조회 종료 월 (`yyyy-MM`) |
| `entries` | array | 게임이 있는 달만 포함 (게임 없는 달은 생략) |
| `entries[].yearMonth` | string | 연월 (`yyyy-MM`) |
| `entries[].timeClass` | string | 타임클래스 |
| `entries[].rating` | number | 해당 월 마지막 게임 레이팅 |