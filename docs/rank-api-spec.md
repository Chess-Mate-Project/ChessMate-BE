# Rank API Spec

## GET /api/rank/ranking

랭킹 목록과 로그인한 사용자의 순위 정보를 조회합니다.

---

### Request

#### Authentication
- 선택(Optional) — `Authorization: Bearer <JWT>`
- 비로그인 상태로 호출 가능하며, 이 경우 `myRankInfo.loggedInUser = false`

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `platform` | `OAuthPlatForm` | **필수** | — | 조회할 체스 플랫폼. `LICHESS` \| `CHESSCOM` |
| `gameType` | `GameType` | 선택 | `RAPID` | 게임 타입. `RAPID` \| `BLITZ` \| `BULLET` \| `CLASSICAL` |
| `page` | `int` | 선택 | `0` | 페이지 번호 (0-indexed) |
| `size` | `int` | 선택 | `20` | 페이지 당 항목 수. 최솟값 1 (서비스에서 클램핑) |

#### Example Request

```
GET /api/rank/ranking?platform=LICHESS&gameType=RAPID&page=0&size=20
Authorization: Bearer eyJhbGci...
```

---

### Response

#### HTTP Status

| 코드 | 설명 |
|------|------|
| `200 OK` | 정상 응답 |

#### Response Body

```json
{
  "success": true,
  "message": "Ranking 조회 성공",
  "data": {
    "myRankInfo": { ... },
    "ranking": [ ... ],
    "totalCount": 1500,
    "currentPage": 0,
    "pageSize": 20,
    "totalPages": 75
  }
}
```

---

### myRankInfo 상세

현재 로그인한 사용자의 순위 정보. 상태에 따라 아래 세 가지 케이스 중 하나.

#### Case 1 — 비로그인

```json
{
  "loggedInUser": false,
  "platformMismatch": false,
  "rank": 0,
  "rating": 0,
  "userId": null,
  "username": null,
  "banner": null,
  "profile": null,
  "description": null
}
```

#### Case 2 — 로그인했지만 조회 플랫폼과 가입 플랫폼이 다름

`loggedInUser=true`, `platformMismatch=true`. 사용자 정보는 가입 플랫폼 기준으로 채워짐.  
`rank`와 `rating`은 해당 플랫폼에 데이터가 없으므로 `0`.

```json
{
  "loggedInUser": true,
  "platformMismatch": true,
  "rank": 0,
  "rating": 0,
  "userId": 42,
  "username": "nuza",
  "banner": "https://cdn.example.com/banner/42.jpg",
  "profile": "https://cdn.example.com/profile/42.jpg",
  "description": "체스를 좋아합니다"
}
```

#### Case 3 — 로그인 + 플랫폼 일치 + 게임 이력 없음

`rank=0`, `rating=0`. 사용자 정보는 채워짐.

```json
{
  "loggedInUser": true,
  "platformMismatch": false,
  "rank": 0,
  "rating": 0,
  "userId": 42,
  "username": "nuza",
  "banner": "https://cdn.example.com/banner/42.jpg",
  "profile": "https://cdn.example.com/profile/42.jpg",
  "description": "체스를 좋아합니다"
}
```

#### Case 4 — 로그인 + 플랫폼 일치 + 게임 이력 있음

```json
{
  "loggedInUser": true,
  "platformMismatch": false,
  "rank": 15,
  "rating": 1850,
  "userId": 42,
  "username": "nuza",
  "banner": "https://cdn.example.com/banner/42.jpg",
  "profile": "https://cdn.example.com/profile/42.jpg",
  "description": "체스를 좋아합니다"
}
```

#### myRankInfo 필드 정의

| 필드 | 타입 | 설명 |
|------|------|------|
| `loggedInUser` | `boolean` | 로그인 여부 |
| `platformMismatch` | `boolean` | 로그인은 됐지만 가입 플랫폼 ≠ 조회 플랫폼 |
| `rank` | `int` | 내 순위. 이력 없음 또는 플랫폼 불일치 시 `0` |
| `rating` | `int` | 내 레이팅. 이력 없음 또는 플랫폼 불일치 시 `0` |
| `userId` | `Long` | 유저 PK. 비로그인 시 `null` |
| `username` | `String` | 유저명. 비로그인 시 `null` |
| `banner` | `String` | 배너 이미지 URL. 없으면 `null` |
| `profile` | `String` | 프로필 이미지 URL. 없으면 `null` |
| `description` | `String` | 자기소개. 없으면 `null` |

---

### ranking 배열 항목

```json
[
  {
    "userId": 7,
    "username": "grandmaster_kim",
    "description": "FIDE 2400",
    "rating": 2380,
    "rank": 1,
    "bannerImage": "https://cdn.example.com/banner/7.jpg",
    "profileImage": "https://cdn.example.com/profile/7.jpg"
  },
  {
    "userId": 42,
    "username": "nuza",
    "description": "체스를 좋아합니다",
    "rating": 1850,
    "rank": 2,
    "bannerImage": null,
    "profileImage": "https://cdn.example.com/profile/42.jpg"
  }
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `userId` | `Long` | 유저 PK |
| `username` | `String` | 유저명. 데이터 없을 시 `"Unknown"` |
| `description` | `String` | 자기소개. 없으면 `null` |
| `rating` | `int` | 해당 gameType 레이팅 |
| `rank` | `int` | 순위 (1-indexed) |
| `bannerImage` | `String` | 배너 이미지 URL. 없으면 `null` |
| `profileImage` | `String` | 프로필 이미지 URL. 없으면 `null` |

---

### 페이지네이션 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalCount` | `long` | 전체 랭킹 유저 수 |
| `currentPage` | `int` | 현재 페이지 (0-indexed) |
| `pageSize` | `int` | 실제 적용된 페이지 크기 (최소 1) |
| `totalPages` | `long` | 전체 페이지 수 = `ceil(totalCount / pageSize)` |

---

### 순위 정렬 기준

```
1순위: rating DESC
2순위(동점): userId ASC
```

동점자 중 먼저 가입한(userId가 작은) 사용자가 더 높은 순위를 가집니다.

---

### 상태 판단 흐름

```
요청
 │
 ├─ userId == null 또는 userProvider == null
 │    → myRankInfo: { loggedInUser=false }
 │
 ├─ userProvider != platform (플랫폼 불일치)
 │    → myRankInfo: { loggedInUser=true, platformMismatch=true, rank=0, rating=0,
 │                    사용자 정보는 가입 플랫폼에서 조회 }
 │
 ├─ 게임 이력 없음
 │    → myRankInfo: { loggedInUser=true, platformMismatch=false, rank=0, rating=0,
 │                    사용자 정보 채움 }
 │
 └─ 정상
      → myRankInfo: { loggedInUser=true, platformMismatch=false, rank=N, rating=R,
                      사용자 정보 채움 }
```