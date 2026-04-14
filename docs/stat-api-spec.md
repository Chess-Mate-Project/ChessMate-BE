# Stat API 명세서

## 공통 사항

- 모든 요청은 JWT 인증 필요 (쿠키 또는 `Authorization: Bearer {token}`)
- 성공 응답 포맷: `{ "message": "...", "data": { ... } }`
- **유효한 `timeClass` 값:** `bullet` / `blitz` / `rapid` / `classical`
- **유효한 `platform` 값:** `LICHESS` / `CHESSCOM`

---

## 엔드포인트 목록

| 메서드 | 경로 | timeClass 지원 | 설명 |
|--------|------|:--------------:|------|
| GET | `/api/stat/streak` | ✗ | 연도별 일별 게임 스트릭 |
| GET | `/api/stat/color` | ✓ | 색상 × 타임클래스별 승/무/패 |
| GET | `/api/stat/first-move` | ✓ | 첫 수 × 색상 × 타임클래스별 빈도 |
| GET | `/api/stat/perf` | ✓ | 타임클래스별 레이팅 + 승/무/패 |

---

## GET /api/stat/streak

일별 게임 수를 연도별로 그룹화하여 반환. `year` 생략 시 전체 연도 반환.

**쿼리 파라미터**

| 파라미터 | 필수 | 설명 |
|---------|:----:|------|
| `platform` | O | `LICHESS` \| `CHESSCOM` |
| `year` | X | 생략 시 전체 연도 / 지정 시 해당 연도만 |

**요청 예시**
```
# 전체 연도
GET /api/stat/streak?platform=LICHESS

# 2024년만
GET /api/stat/streak?platform=LICHESS&year=2024
```

**응답**
```json
{
  "message": "게임 스트릭 조회 성공",
  "data": [
    {
      "year": 2023,
      "days": [
        { "date": "2023-05-10", "total": 6, "wins": 3, "draws": 1, "losses": 2 }
      ]
    },
    {
      "year": 2024,
      "days": [
        { "date": "2024-01-03", "total": 5, "wins": 3, "draws": 1, "losses": 1 },
        { "date": "2024-03-15", "total": 8, "wins": 4, "draws": 2, "losses": 2 }
      ]
    }
  ]
}
```

> 게임을 하지 않은 날짜는 포함되지 않음. 날짜 오름차순 정렬.

---

## GET /api/stat/color

색상(WHITE / BLACK) × 타임클래스별 승/무/패 통계.

**쿼리 파라미터**

| 파라미터 | 필수 | 설명 |
|---------|:----:|------|
| `platform` | O | `LICHESS` \| `CHESSCOM` |
| `timeClass` | X | 생략 시 전체 타입 반환 |

**요청 예시**
```
# 전체 타입
GET /api/stat/color?platform=LICHESS

# 블리츠만
GET /api/stat/color?platform=LICHESS&timeClass=blitz
```

**응답**
```json
{
  "message": "색상별 게임 통계 조회 성공",
  "data": [
    { "timeClass": "blitz", "color": "WHITE", "total": 200, "wins": 110, "draws": 30, "losses": 60 },
    { "timeClass": "blitz", "color": "BLACK", "total": 190, "wins": 95,  "draws": 25, "losses": 70 }
  ]
}
```

---

## GET /api/stat/first-move

첫 수 × 색상 × 타임클래스별 빈도 통계.

**쿼리 파라미터**

| 파라미터 | 필수 | 설명 |
|---------|:----:|------|
| `platform` | O | `LICHESS` \| `CHESSCOM` |
| `timeClass` | X | 생략 시 전체 타입 반환 |

**요청 예시**
```
# 전체 타입
GET /api/stat/first-move?platform=LICHESS

# 블리츠만
GET /api/stat/first-move?platform=LICHESS&timeClass=blitz
```

**응답**
```json
{
  "message": "첫 수 통계 조회 성공",
  "data": [
    { "timeClass": "blitz", "color": "WHITE", "move": "e4", "count": 120 },
    { "timeClass": "blitz", "color": "WHITE", "move": "d4", "count": 80 },
    { "timeClass": "blitz", "color": "BLACK", "move": "e5", "count": 95 }
  ]
}
```

---

## GET /api/stat/perf

타임클래스별 레이팅 + 승/무/패 통계. 플랫폼 API에서 직접 가져온 레이팅 포함.

**쿼리 파라미터**

| 파라미터 | 필수 | 설명 |
|---------|:----:|------|
| `platform` | O | `LICHESS` \| `CHESSCOM` |
| `timeClass` | X | 생략 시 전체 타입 반환 / 지정 시 해당 타입만 |

**요청 예시**
```
# 전체 타입
GET /api/stat/perf?platform=LICHESS

# 블리츠만
GET /api/stat/perf?platform=LICHESS&timeClass=blitz

# Chess.com 래피드
GET /api/stat/perf?platform=CHESSCOM&timeClass=rapid
```

**응답**
```json
{
  "message": "퍼프 통계 조회 성공",
  "data": [
    { "timeClass": "bullet",    "rating": 1850, "games": 320, "wins": 170, "losses": 120, "draws": 30 },
    { "timeClass": "blitz",     "rating": 1920, "games": 480, "wins": 250, "losses": 180, "draws": 50 },
    { "timeClass": "rapid",     "rating": 1780, "games": 200, "wins": 105, "losses":  75, "draws": 20 },
    { "timeClass": "classical", "rating": 1700, "games":  40, "wins":  20, "losses":  15, "draws":  5 }
  ]
}
```

**데이터 소스**

| 플랫폼 | 레이팅 | 승/무/패 |
|--------|--------|----------|
| Lichess | `GET /api/user/{username}` → `perfs.{timeClass}.rating` | `user_color_stat` WHITE + BLACK 합산 |
| Chess.com | `GET /pub/player/{username}/stats` → `chess_{timeClass}.last.rating` | 동일 API `record` 필드 |

> `user_perf_stat` 테이블은 게임 수집 완료 시 `PerfStatFetcher`에 의해 자동 갱신됨.

---

## timeClass 필터 동작 정리

```
timeClass 미지정    → 전체 타입 데이터 반환
timeClass=bullet    → bullet 게임만
timeClass=blitz     → blitz 게임만
timeClass=rapid     → rapid 게임만
timeClass=classical → classical 게임만
```

> `color`, `first-move`: DB WHERE 절 필터  
> `perf`: 전체 조회 후 Java 스트림 필터 (레코드 수가 최대 4개이므로 성능 문제 없음)
