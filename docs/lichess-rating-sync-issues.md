# Lichess 게임 동기화 — 레이팅 관련 이슈 분석 및 대응

---

## 1. Fix: 게임 후 레이팅(exit rating) 저장

### 문제

Lichess NDJSON의 `Player.rating`은 게임 **시작 전** 레이팅입니다.

```
[BlackElo "1500"]         ← 게임 시작 시 레이팅 (entry rating)
[BlackRatingDiff "-226"]  ← 이 게임으로 인한 변화
```

기존 코드는 `Player.rating`만 저장 → 실제 게임 후 레이팅과 괴리 발생.  
월별 마지막 레이팅 조회 시 마지막 게임의 entry rating이 반환되어 ratingDiff만큼 오차가 생깁니다.

### Fix

```java
// LichessGameSyncWorker.resolveRating()
int diff = player.ratingDiff() != null ? player.ratingDiff() : 0;
return player.rating() + diff;
```

- **rated 게임**: `rating + ratingDiff` = 게임 후 실제 레이팅
- **casual 게임**: `ratingDiff = null` → diff = 0 → entry rating 그대로 (레이팅 변화 없음)

Chess.com은 `ChesscomGamePlayerInfo.rating`이 이미 게임 후 정산 레이팅을 반환하므로 변경 없음.

---

## 2. 이슈: Lichess `rated=true` 파라미터 신뢰성 문제

### 관찰

| 요청 | 반환 게임 수 | 설명 |
|---|---|---|
| `?perf=rapid,bullet,classical,blitz` | 40+ 게임 | casual + rated 혼재, 2020년까지 |
| `?perf=rapid,bullet,classical,blitz&rated=true` | **1게임** | 2026 blitz 1개만 |

2020년 `[Event "rated classical game"]`, `[Event "rated rapid game"]` (RatingDiff 존재) 게임들이 `rated=true` 추가 시 사라집니다.

### 원인 분석

**`rated=true` 자체의 문제가 아닌, `perf 필터 + rated` 조합 문제입니다.**

Lichess API의 `perf` 파라미터는 **내부 perf 분류 기준**으로 필터링합니다. 2020년 게임들의 경우:

- `[TimeControl "900+180"]` → 예상 게임 시간: 900 + 180×40 = 8100초 (135분)
- PGN에는 "classical"로 표시되지만, Lichess **내부 시스템**에서 해당 게임들이 다른 perf 카테고리로 분류되었을 가능성이 높습니다
- `perf=classical`로 쿼리해도 매칭되지 않아 결과에서 제외됩니다

즉 Lichess의 **perf 내부 분류 기준이 PGN event 이름 / 타임컨트롤과 항상 일치하지 않습니다.**

### 우리 코드의 추가 문제점

현재 `LichessGameSyncWorker`는 API의 `rated=true`에만 의존합니다:

```java
// API 파라미터로만 필터링
lichessApi.getGames(..., rated=true)

// 클라이언트 사이드 재검증 없음
List<Game> toSave = games.stream()
    .filter(g -> SUPPORTED_TIME_CLASSES.contains(g.perf()))  // perf만 확인
    .filter(g -> g.createdAt() != null && g.createdAt() > 0)
    .map(g -> toGame(...))
    .toList();
```

- API `rated=true`가 일부 rated 게임을 누락시키면 → **DB에 데이터 없음 (false negative)**
- API가 일부 casual 게임을 rated로 잘못 반환하면 → **casual 게임이 rated로 저장됨 (false positive)**

---

## 3. 대안

### 방안 A: `rated=true` 제거 + 클라이언트 사이드 이중 필터 (권장)

```java
// API 호출 시 rated 파라미터 제거
lichessApi.getGames(..., rated=null)

// 클라이언트 사이드에서 정확하게 필터
List<Game> toSave = games.stream()
    .filter(g -> g.rated())                                   // NDJSON의 rated 필드 직접 확인
    .filter(g -> SUPPORTED_TIME_CLASSES.contains(g.perf()))
    .filter(g -> g.createdAt() != null && g.createdAt() > 0)
    .map(g -> toGame(...))
    .toList();
```

**장점**
- API 파라미터 신뢰성 문제 완전 회피
- NDJSON의 `rated` 필드는 각 게임의 실제 rated 여부를 나타내므로 신뢰 가능
- 2020년처럼 perf 분류가 불분명한 구형 게임도 정확히 처리

**단점**
- API 응답량 증가 (casual 게임도 받아온 후 버림)
- 트래픽 증가, 수집 속도 소폭 저하

### 방안 B: `rated=true` 유지 + 클라이언트 사이드 이중 검증 (보수적)

```java
// API 파라미터는 유지 (1차 필터 역할)
lichessApi.getGames(..., rated=true)

// 클라이언트에서도 재검증 (2차 필터)
.filter(g -> g.rated())
```

**장점**: 트래픽 유지  
**단점**: API false negative 문제(일부 rated 게임 누락) 해결 불가

### 방안 C: `GameStatAggregator`에서 `game.rated` 기반 필터 강화

현재 집계 시 이미 `Boolean.TRUE.equals(g.getRated())`로 필터링하므로 stats는 안전합니다. 다만 rating history 쿼리에 `AND g.rated = true` 조건 추가 필요:

```java
// GameJpaRepository JPQL
AND g.rated = true
```

---

## 4. 결론 및 조치 현황

| 항목 | 상태 | 내용 |
|---|---|---|
| exit rating 저장 | ✅ 완료 | `rating + ratingDiff` 저장 |
| API rated 필터 신뢰성 | ✅ 완료 (방안 A) | `rated=null` + 클라이언트 `g.rated()` 필터 |
| rating history 쿼리 | ✅ 완료 | JPQL에 `AND g.rated = true` 추가 |
