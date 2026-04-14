# Frontend API 변경사항 전달 문서

> 최종 업데이트: 2026-04-08

---

## 목차
1. [프로필 API 변경](#1-프로필-api-변경)
2. [통계 API 변경](#2-통계-api-변경)
3. [이미지 API](#3-이미지-api)
4. [전체 엔드포인트 목록](#4-전체-엔드포인트-목록)

---

## 1. 프로필 API 변경

### GET /api/user/profile

사용자 프로필 조회. **`platformJoinedAt` 필드 추가됨.**

**요청**
```
GET /api/user/profile
Authorization: 쿠키(자동)
```

**응답**
```json
{
  "message": "프로필 조회 성공",
  "data": {
    "id": 1,
    "username": "hikaru",
    "platform": "LICHESS",
    "description": "안녕하세요",
    "profile_image_url": "https://pub-xxxx.r2.dev/users/lichess/1/profile.jpg",
    "banner_image_url": "https://pub-xxxx.r2.dev/default/default_banner.png",
    "created_at": "2026-01-15T09:30:00",
    "platform_joined_at": "2015-03-01T00:00:00"
  }
}
```

**필드 설명**

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | ChessLadder 내부 사용자 ID |
| `username` | String | 플랫폼 닉네임 |
| `platform` | String | `"LICHESS"` 또는 `"CHESSCOM"` |
| `description` | String | 자기소개 (null 가능) |
| `profile_image_url` | String | 프로필 이미지 URL. 미설정 시 기본 이미지 자동 반환 |
| `banner_image_url` | String | 배너 이미지 URL. 미설정 시 기본 이미지 자동 반환 |
| `created_at` | LocalDateTime | **ChessLadder 가입일** |
| `platform_joined_at` | LocalDateTime | **플랫폼(Lichess/Chess.com) 계정 가입일** (null 가능) |

> **`platform_joined_at` 활용**: 스트릭 조회 콤보박스의 시작 연도로 사용하세요.  
> 예) `platform_joined_at`이 `2018-05-12`이면 → 콤보박스 `[2018, 2019, ..., 2026]`

---

### GET /api/auth/me

로그인 상태 확인. **`profile_image_url`이 CDN URL로 변경됨** (이전에는 raw key가 내려갔을 수 있음).

**응답**
```json
{
  "message": "...",
  "data": {
    "user_id": 1,
    "username": "hikaru",
    "platform": "LICHESS",
    "profile_image_url": "https://pub-xxxx.r2.dev/users/lichess/1/profile.jpg",
    "description": "안녕하세요"
  }
}
```

> 프로필 이미지가 없는 신규 가입자: `"https://pub-xxxx.r2.dev/default/default_profile.png"` 자동 반환

---

### PUT /api/user/description

자기소개 수정 (변경 없음, 참고용).

**요청**
```json
{ "description": "새 소개글" }
```

---

## 2. 통계 API 변경

### ⚠️ 삭제된 엔드포인트

| 삭제된 경로 | 대체 경로 |
|------------|----------|
| `GET /api/stat/lichess-summary` | `GET /api/stat/perf?platform=LICHESS` |
| `GET /api/stat/chesscom-summary` | `GET /api/stat/perf?platform=CHESSCOM` |

---

### GET /api/stat/perf ✨ 신규

타임클래스별 **레이팅 + 승/무/패** 통계.

**요청**
```
GET /api/stat/perf?platform=LICHESS
GET /api/stat/perf?platform=LICHESS&timeClass=blitz
GET /api/stat/perf?platform=CHESSCOM
GET /api/stat/perf?platform=CHESSCOM&timeClass=rapid
```

**응답**
```json
{
  "message": "퍼프 통계 조회 성공",
  "data": [
    {
      "time_class": "bullet",
      "rating": 1850,
      "games": 320,
      "wins": 170,
      "losses": 120,
      "draws": 30
    },
    {
      "time_class": "blitz",
      "rating": 1920,
      "games": 480,
      "wins": 250,
      "losses": 180,
      "draws": 50
    },
    {
      "time_class": "rapid",
      "rating": 1780,
      "games": 200,
      "wins": 105,
      "losses": 75,
      "draws": 20
    },
    {
      "time_class": "classical",
      "rating": 1700,
      "games": 40,
      "wins": 20,
      "losses": 15,
      "draws": 5
    }
  ]
}
```

> - `timeClass` 생략 시 전체 타입 배열 반환
> - `games = wins + draws + losses` (항상 일치함)
> - 게임 기록이 없는 타임클래스는 배열에 포함되지 않음

---

### GET /api/stat/streak

연도별 일별 게임 스트릭.

**요청**
```
GET /api/stat/streak?platform=LICHESS              → 전체 연도
GET /api/stat/streak?platform=LICHESS&year=2024    → 2024년만
```

**응답**
```json
{
  "message": "게임 스트릭 조회 성공",
  "data": [
    {
      "year": 2024,
      "days": [
        { "date": "2024-01-03", "total": 5, "wins": 3, "draws": 1, "losses": 1 },
        { "date": "2024-03-15", "total": 8, "wins": 4, "draws": 2, "losses": 2 }
      ]
    },
    {
      "year": 2025,
      "days": [
        { "date": "2025-02-10", "total": 6, "wins": 3, "draws": 1, "losses": 2 }
      ]
    }
  ]
}
```

> - 게임을 하지 않은 날짜는 포함되지 않음 (잔디 달력 구현 시 나머지 날은 0으로 처리 필요)
> - `year` 파라미터와 `platform_joined_at` 연도를 조합하여 연도 콤보박스 구현 권장

---

### GET /api/stat/color

색상(WHITE / BLACK) × 타임클래스별 승/무/패.

**요청**
```
GET /api/stat/color?platform=LICHESS
GET /api/stat/color?platform=LICHESS&timeClass=blitz
```

**응답**
```json
{
  "message": "색상별 게임 통계 조회 성공",
  "data": [
    { "time_class": "blitz", "color": "WHITE", "wins": 110, "draws": 30, "losses": 60 },
    { "time_class": "blitz", "color": "BLACK", "wins": 95,  "draws": 25, "losses": 70 }
  ]
}
```

---

### GET /api/stat/first-move

첫 수 × 색상 × 타임클래스별 빈도.

**요청**
```
GET /api/stat/first-move?platform=LICHESS
GET /api/stat/first-move?platform=LICHESS&timeClass=blitz
```

**응답**
```json
{
  "message": "첫 수 통계 조회 성공",
  "data": [
    { "time_class": "blitz", "color": "WHITE", "move": "e4", "count": 120 },
    { "time_class": "blitz", "color": "WHITE", "move": "d4", "count": 80 },
    { "time_class": "blitz", "color": "BLACK", "move": "e5", "count": 95 }
  ]
}
```

---

## 3. 이미지 API

### 이미지 없을 때 기본값

프로필/배너 이미지가 없는 경우 서버에서 자동으로 기본 이미지 URL을 반환합니다.

```
프로필 기본: https://pub-xxxx.r2.dev/default/default_profile.png
배너 기본:   https://pub-xxxx.r2.dev/default/default_banner.png
```

프론트에서 null 체크 불필요. 항상 유효한 URL이 내려옵니다.

---

## 4. 전체 엔드포인트 목록

### 인증

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/auth/me` | 로그인 상태 확인 |
| POST | `/api/auth/logout` | 로그아웃 |
| POST | `/api/auth/refresh` | 토큰 갱신 |

### 유저

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/user/profile` | 프로필 조회 (`platform_joined_at` 포함) |
| PUT | `/api/user/description` | 자기소개 수정 |

### 통계

| 메서드 | 경로 | 파라미터 | 설명 |
|--------|------|----------|------|
| GET | `/api/stat/perf` | `platform`, `timeClass`(선택) | 타임클래스별 레이팅+승패무 |
| GET | `/api/stat/streak` | `platform`, `year`(선택) | 연도별 일별 게임 스트릭 |
| GET | `/api/stat/color` | `platform`, `timeClass`(선택) | 색상별 승패무 |
| GET | `/api/stat/first-move` | `platform`, `timeClass`(선택) | 첫 수 빈도 |

### 공통 파라미터

| 파라미터 | 유효값 |
|----------|--------|
| `platform` | `LICHESS` \| `CHESSCOM` |
| `timeClass` | `bullet` \| `blitz` \| `rapid` \| `classical` |

---

## 스트릭 콤보박스 구현 가이드

```
1. GET /api/user/profile 호출
2. platform_joined_at 에서 연도 추출 (예: 2018)
3. 콤보박스 = [2018, 2019, ..., 현재연도]
4. 선택한 연도로 GET /api/stat/streak?platform=LICHESS&year=2018 호출
5. 응답의 days 배열을 달력에 렌더링 (없는 날은 0으로 처리)
```

> `platform_joined_at`이 null인 경우: 현재 연도만 표시하거나, 가능한 연도 범위를 fallback 처리 권장
