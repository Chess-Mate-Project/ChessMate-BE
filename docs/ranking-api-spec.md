# 랭킹 API 명세서

> 작성일: 2026-04-16  
> Base URL: `/api/rank`  
> 응답 필드명 규칙: `snake_case` (Jackson `SNAKE_CASE` 전략 적용)

---

## 엔드포인트 목록

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/api/rank/ranking` | 선택 | 플랫폼 + 게임타입별 랭킹 조회 |

---

## GET `/api/rank/ranking`

### 설명

플랫폼(Chess.com / Lichess)과 게임 타입(Rapid / Blitz / Bullet / Classical) 기준의 랭킹을 페이지 단위로 조회합니다.

- 로그인 사용자: 본인 순위(`my_rank_info`) + 해당 페이지 랭킹 목록 반환
- 비로그인 사용자: 빈 응답 반환 (랭킹 목록 조회 불가)

---

### Request

#### Headers

| 헤더 | 필수 | 설명 |
|------|------|------|
| `Authorization` | 선택 | `Bearer {accessToken}` |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `platform` | `String` | **Y** | - | `LICHESS` 또는 `CHESSCOM` |
| `gameType` | `String` | N | `RAPID` | `RAPID` \| `BLITZ` \| `BULLET` \| `CLASSICAL` |
| `page` | `int` | N | `0` | 페이지 번호 (0-indexed) |
| `size` | `int` | N | `20` | 페이지 크기 |

#### 요청 예시

```
GET /api/rank/ranking?platform=LICHESS&gameType=RAPID&page=0&size=20
Authorization: Bearer eyJhbGci...
```

---

### Response

#### 공통 래퍼

```json
{
  "success": true,
  "message": "Ranking 조회 성공",
  "data": { ... }
}
```

#### `data` 필드 상세

| 필드 | 타입 | 설명 |
|------|------|------|
| `my_rank_info` | `Object` | 요청자 본인 순위 정보 |
| `ranking` | `Array` | 해당 페이지의 랭커 목록 |
| `total_count` | `long` | 전체 랭커 수 |
| `current_page` | `int` | 현재 페이지 번호 (0-indexed) |
| `page_size` | `int` | 페이지 크기 |
| `total_pages` | `long` | 전체 페이지 수 |

#### `my_rank_info` 필드 상세

| 필드 | 타입 | 설명 |
|------|------|------|
| `logged_in_user` | `boolean` | 로그인 여부 |
| `rank` | `int` | 내 순위. `0` = 해당 게임타입 이력 없음 |
| `rating` | `int` | 내 레이팅. 이력 없으면 `0` |
| `user_id` | `long` | 사용자 ID |
| `username` | `string` | 사용자명 |
| `banner` | `string` | 배너 이미지 CDN URL |
| `profile` | `string` | 프로필 이미지 CDN URL |
| `description` | `string` | 자기소개 |

#### `ranking[]` 항목 필드 상세

| 필드 | 타입 | 설명 |
|------|------|------|
| `user_id` | `long` | 사용자 ID |
| `username` | `string` | 사용자명 |
| `description` | `string` | 자기소개 |
| `rating` | `int` | 레이팅 |
| `rank` | `int` | 순위 (1-indexed, 전체 기준) |
| `banner_image` | `string` | 배너 이미지 CDN URL |
| `profile_image` | `string` | 프로필 이미지 CDN URL |

---

### Response 예시

#### 로그인 사용자 (200 OK)

```json
{
  "success": true,
  "message": "Ranking 조회 성공",
  "data": {
    "my_rank_info": {
      "logged_in_user": true,
      "rank": 42,
      "rating": 1580,
      "user_id": 7,
      "username": "nuza",
      "banner": "https://cdn.chessmate.io/users/lichess/7/banner.jpg",
      "profile": "https://cdn.chessmate.io/users/lichess/7/profile.jpg",
      "description": "Chess lover"
    },
    "ranking": [
      {
        "user_id": 1,
        "username": "GrandMaster_Kim",
        "description": "",
        "rating": 2400,
        "rank": 1,
        "banner_image": "https://cdn.chessmate.io/users/lichess/1/banner.jpg",
        "profile_image": "https://cdn.chessmate.io/users/lichess/1/profile.jpg"
      },
      {
        "user_id": 3,
        "username": "BlitzKing",
        "description": "Rapid specialist",
        "rating": 2350,
        "rank": 2,
        "banner_image": "https://cdn.chessmate.io/default/default_banner.png",
        "profile_image": "https://cdn.chessmate.io/users/lichess/3/profile.jpg"
      }
    ],
    "total_count": 1250,
    "current_page": 0,
    "page_size": 20,
    "total_pages": 63
  }
}
```

#### 비로그인 사용자 (200 OK)

```json
{
  "success": true,
  "message": "Ranking 조회 성공",
  "data": {
    "my_rank_info": {
      "logged_in_user": false,
      "rank": 0,
      "rating": 0,
      "user_id": null,
      "username": null,
      "banner": null,
      "profile": null,
      "description": null
    },
    "ranking": [],
    "total_count": 0,
    "current_page": 0,
    "page_size": 20,
    "total_pages": 0
  }
}
```

#### 해당 게임타입 이력이 없는 사용자 (200 OK)

`rank = 0`, `rating = 0`으로 반환. 랭킹 목록은 정상 조회됨.

```json
{
  "data": {
    "my_rank_info": {
      "logged_in_user": true,
      "rank": 0,
      "rating": 0,
      "user_id": 7,
      "username": "nuza",
      "banner": "https://cdn.chessmate.io/...",
      "profile": "https://cdn.chessmate.io/..."
    },
    "ranking": [ ... ],
    "total_count": 1250,
    "current_page": 0,
    "page_size": 20,
    "total_pages": 63
  }
}
```

---

### Error Response

#### `platform` 파라미터 누락 (400 Bad Request)

```json
{
  "success": false,
  "message": "Required request parameter 'platform' is not present"
}
```

#### `gameType` 값 오류 (400 Bad Request)

`RAPID`, `BLITZ`, `BULLET`, `CLASSICAL` 외의 값 입력 시.

```json
{
  "success": false,
  "message": "No enum constant com.chessmate.common.type.GameType.INVALID"
}
```

---

### 페이지네이션 동작

| 조건 | 동작 |
|------|------|
| `page=0&size=20` | 1~20위 반환 |
| `page=1&size=20` | 21~40위 반환 |
| `page=62&size=20` | 1241~1250위 반환 (마지막 페이지) |
| `page=999&size=20` | `ranking = []`, `total_count`는 정상 반환 |

`rank` 값은 페이지와 무관하게 **전체 기준 절대 순위** (`page=2&size=20`이면 41위부터 시작).

---

### 정렬 기준

1. **`rating` 내림차순** — 레이팅 높은 순
2. **`user_id` 오름차순** — 동점자는 userId가 작은 순 (먼저 가입한 순)

---

### 비고

- 이미지가 없는 경우 CDN 기본 이미지 URL 반환
  - 프로필: `{CDN}/default/default_profile.png`
  - 배너: `{CDN}/default/default_banner.png`
- 현재 캐시 없이 DB 직접 조회 (추후 Redis 캐시 추가 예정)
- 레이팅 데이터는 30분 간격으로 게임 수집 후 집계