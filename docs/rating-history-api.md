# Rating History API

## 엔드포인트

```
GET /api/stat/rating-history?platform={platform}[&timeClass={timeClass}]
```

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `platform` | O | `LICHESS` 또는 `CHESSCOM` |
| `timeClass` | X | `blitz`, `bullet`, `rapid`, `classical` — 생략 시 전체 타임클래스 반환 |

---

## 응답 구조

```json
{
  "from": "2025-05",
  "to": "2026-04",
  "data": [
    { "yearMonth": "2025-05", "timeClass": "blitz",  "rating": 1480 },
    { "yearMonth": "2025-05", "timeClass": "bullet", "rating": 1350 },
    { "yearMonth": "2025-06", "timeClass": "blitz",  "rating": 1510 }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `from` | 조회 시작 월 (오늘 기준 11개월 전) |
| `to` | 조회 종료 월 (이번 달) |
| `data[].yearMonth` | 해당 월 (`yyyy-MM`) |
| `data[].timeClass` | 게임 타입 |
| `data[].rating` | 해당 월 마지막 게임 기준 레이팅 |

- 게임 데이터가 없는 달은 `data`에 포함되지 않음
- `from`~`to` 범위는 항상 12개월 (이번 달 포함)

---

## 데이터 출처

`game` 테이블에서 직접 조회. 별도 집계 테이블 없음.

- 각 `(timeClass, year, month)` 그룹 내에서 `played_at`이 가장 늦은 게임의 `rating` 반환
- `rating` 컬럼은 동기 시점에 플랫폼 API 응답에서 수집
  - Lichess: `players.white.rating` / `players.black.rating`
  - Chess.com: `white.rating` / `black.rating`

---

## 변경된 파일

| 파일 | 변경 내용 |
|---|---|
| `domain/game/Game.java` | `rating` 필드 추가 |
| `domain/game/GameRepository.java` | `findMonthlyLastRating()` 인터페이스 추가 |
| `domain/game/MonthlyRating.java` | 신규 — 쿼리 결과 record |
| `infra/game/GameJpaEntity.java` | `rating` 컬럼 추가 |
| `infra/game/GameMapper.java` | rating 매핑 추가 |
| `infra/game/GameJpaRepository.java` | 월별 마지막 레이팅 JPQL 쿼리 추가 |
| `infra/game/projection/MonthlyRatingProjection.java` | 신규 — JPA Projection 인터페이스 |
| `infra/game/GameRepositoryImpl.java` | `findMonthlyLastRating()` 구현 |
| `worker/LichessGameSyncWorker.java` | `toGame()`에 rating 세팅 |
| `worker/ChessComGameSyncWorker.java` | `toGame()`에 rating 세팅 |
| `api/stat/dto/MonthlyRatingEntry.java` | 신규 |
| `api/stat/dto/RatingHistoryResponse.java` | 신규 |
| `api/stat/service/StatService.java` | `getRatingHistory()` 추가 |
| `api/stat/controller/StatController.java` | 엔드포인트 추가 |
