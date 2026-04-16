# ChessMate BE — 통계/유저/이미지 API 리팩토링 가이드

## 1. 리팩토링 배경 및 목표

기존 코드는 단일 `User` 테이블 + `UserPerf` / `UserDailyStreak` 등 레거시 엔티티 기반이었으며, Lichess 전용으로만 구성되어 있었다. 이번 리팩토링의 목표는 다음과 같다.

- **플랫폼 분리**: `lichess_users` / `chesscom_users` 별도 테이블로 분리
- **통계 테이블 정규화**: 게임 수집(worker) → 집계(aggregator) → 조회(API) 파이프라인 명확화
- **Lichess rated 게임 필터링**: Lichess API `rated=true` 파라미터 + 집계 단계 rated 필터링
- **Cache-Aside 제거**: 통계 API는 캐시 없이 직접 DB 조회 (단순성 우선)
- **이미지 업로드**: Cloudflare R2 Presigned URL 방식으로 직접 업로드

---

## 2. 도메인 구조

### 2.1 유저 테이블

| 테이블 | 설명 |
|--------|------|
| `lichess_users` | Lichess 기본 프로필 (id, lichess_id, username, description, profile, banner, created_at) |
| `chesscom_users` | Chess.com 기본 프로필 (id, chesscom_id, username, description, profile, banner, created_at) |
| `lichess_user_stat` | Lichess 요약 통계 별도 테이블 (1:1, rated 게임 기반 집계) |
| `chesscom_user_stat` | Chess.com 요약 통계 별도 테이블 (1:1, rated 게임 기반 집계) |

### 2.2 통계 테이블 (공통)

| 테이블 | 설명 |
|--------|------|
| `user_daily_game_stat` | 날짜별 게임 수 (platform, date, total/wins/draws/losses) |
| `user_color_stat` | 색상×타임클래스별 승패 통계 |
| `user_first_move_stat` | 첫 수×색상×타임클래스별 빈도 |

---

## 3. 의사결정 이유

### 3.1 lichess_user_stat 분리

Lichess API는 `/api/user/{id}` 에서 전체 통계를 반환하지만, rated/unrated 구분 없이 혼합된 수치다.
정확한 rated 게임 기반 통계를 위해 **Game 테이블에서 직접 집계**하도록 결정. `lichess_users`에서 분리하여 별도 `lichess_user_stat` 테이블에 저장.

### 3.2 Lichess rated=true 파라미터

```java
// LichessApi.java
Mono<String> getGames(String username, Integer max, Boolean rated);
// → rated=true 로 호출하면 API 단에서 필터
```

Worker에서 `rated=true`로 가져오므로 클라이언트 측 필터 불필요.

### 3.3 Chess.com rated 필터

Chess.com API는 rated 파라미터를 지원하지 않아, `GameStatAggregator`에서 집계 시 필터:

```java
List<Game> ratedGames = allGames.stream()
    .filter(g -> Boolean.TRUE.equals(g.getRated()))
    .toList();
```

### 3.4 Cache-Aside 제거

통계 API는 게임 수집(매일 배치) 이후 거의 변하지 않는 데이터. 캐시 무효화 타이밍 복잡도를 줄이기 위해 직접 DB 조회로 단순화. 성능이 문제가 되는 시점에 캐시 레이어 추가 검토.

### 3.5 이미지 Presigned URL 방식

서버를 통한 이미지 전달 대신 클라이언트 → R2 직접 업로드:
- 서버 트래픽/메모리 절약
- 업로드 완료 후 `POST /api/image/upload-complete` 로 DB key 저장

---

## 4. 코드 흐름

### 4.1 게임 수집 → 집계 파이프라인

```
[Scheduler]
    ↓
LichessGameSyncWorker / ChesscomGameSyncWorker
    ↓ LichessApi.getGames(username, max, rated=true)
    ↓ 게임 DB 저장
    ↓
GameStatAggregator.aggregate(userId, platform, allGames)
    ↓ rated 게임만 필터 (Chess.com: 집계 단계, Lichess: API 단에서 이미 필터)
    ↓
UserDailyGameStat / UserColorStat / UserFirstMoveStat 저장
    ↓ (Lichess only)
LichessUserStat 저장 (wins/losses/draws 집계)
```

### 4.2 이미지 업로드 플로우

```
클라이언트
  1. GET /api/image/upload-url?type=PROFILE&contentType=image/jpeg
     → { uploadUrl: "https://..." }
  2. PUT {uploadUrl} (R2에 직접 업로드)
  3. POST /api/image/upload-complete?type=PROFILE
     → DB에 key 저장
  4. GET /api/image/url?type=PROFILE
     → { url: "https://cdn.../users/LICHESS/123/profile.jpg" }
```

---

## 5. 핵심 코드 스니펫

### StatService.java

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatService {
    public List<DailyGameStatResponse> getDailyStat(Long userId, OAuthPlatForm platform) {
        return dailyStatRepository.findByUserIdAndPlatform(userId, platform)
            .stream().map(DailyGameStatResponse::from).toList();
    }
    // getColorStat, getFirstMoveStat 동일 패턴

    public LichessUserStatResponse getLichessUserStat(Long userId) {
        return lichessUserStatRepository.findByUserId(userId)
            .map(LichessUserStatResponse::from)
            .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
    }
}
```

### UserService.java

```java
public ProfileResponse getProfile(Long userId, OAuthPlatForm platform) {
    return switch (platform) {
        case LICHESS -> {
            LichessUser user = lichessUserRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
            yield new ProfileResponse(user.getId(), user.getUsername(), OAuthPlatForm.LICHESS,
                user.getDescription(),
                imageUtil.getProfileImageUrl(user.getId(), user.getProfile()),
                imageUtil.getBannerImageUrl(user.getId(), user.getBanner()),
                user.getCreatedAt());
        }
        case CHESSCOM -> { /* 동일 */ }
    };
}
```

---

## 6. API 명세서

> **공통 사항**
> - 모든 요청은 `Authorization: Bearer {accessToken}` 쿠키 또는 헤더 필요
> - 성공 응답 포맷: `{ "message": "...", "data": { ... } }`

---

### 6.1 유저 API

#### `GET /api/user/profile`

내 프로필 조회. JWT에서 플랫폼 자동 추출.

**응답**
```json
{
  "message": "프로필 조회 성공",
  "data": {
    "id": 1,
    "username": "magnuscarlsen",
    "platform": "LICHESS",
    "description": "안녕하세요",
    "profileImageUrl": "https://cdn.example.com/users/LICHESS/1/profile.jpg",
    "bannerImageUrl": "https://cdn.example.com/default/default_banner.png",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

#### `PUT /api/user/description`

자기소개 수정.

**요청 Body**
```json
{ "description": "체스를 사랑합니다" }
```

**응답**
```json
{ "message": "자기소개 수정 성공", "data": null }
```

---

### 6.2 이미지 API

#### `GET /api/image/upload-url`

이미지 업로드용 Presigned URL 발급.

**쿼리 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `type` | `PROFILE` \| `BANNER` | O | 이미지 종류 |
| `contentType` | string | O | `image/jpeg`, `image/png`, `image/webp` 중 하나 |

**응답**
```json
{
  "message": "이미지 업로드 URL 생성 성공",
  "data": { "url": "https://r2-presigned-url..." }
}
```

---

#### `POST /api/image/upload-complete`

R2 업로드 완료 후 DB에 key 저장.

**쿼리 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `type` | `PROFILE` \| `BANNER` | O | 이미지 종류 |

**응답**
```json
{ "message": "업로드 완료", "data": null }
```

---

#### `GET /api/image/url`

현재 이미지 URL 조회. key가 없으면 기본 이미지 URL 반환.

**쿼리 파라미터**

| 파라미터 | 타입 | 필수 |
|----------|------|------|
| `type` | `PROFILE` \| `BANNER` | O |

**응답**
```json
{
  "message": "이미지 URL 조회 성공",
  "data": { "url": "https://cdn.example.com/users/LICHESS/1/profile.jpg" }
}
```

---

### 6.3 통계 API

#### `GET /api/stat/daily?platform={platform}`

날짜별 게임 통계.

**쿼리 파라미터**

| 파라미터 | 타입 | 예시 |
|----------|------|------|
| `platform` | `LICHESS` \| `CHESSCOM` | `LICHESS` |

**응답**
```json
{
  "message": "날짜별 게임 통계 조회 성공",
  "data": [
    {
      "date": "2024-11-01",
      "total": 10,
      "wins": 5,
      "draws": 2,
      "losses": 3
    }
  ]
}
```

---

#### `GET /api/stat/color?platform={platform}`

색상×타임클래스별 승패 통계.

**응답**
```json
{
  "message": "색상별 게임 통계 조회 성공",
  "data": [
    {
      "timeClass": "blitz",
      "color": "white",
      "total": 50,
      "wins": 25,
      "draws": 10,
      "losses": 15
    }
  ]
}
```

---

#### `GET /api/stat/first-move?platform={platform}`

첫 수×색상×타임클래스별 빈도 통계.

**응답**
```json
{
  "message": "첫 수 통계 조회 성공",
  "data": [
    {
      "timeClass": "blitz",
      "color": "white",
      "move": "e4",
      "count": 120
    }
  ]
}
```

---

#### `GET /api/stat/chesscom-summary`

Chess.com 전용 요약 통계 (rated 게임 기반).

> Lichess는 해당 엔드포인트 없음.

**응답**
```json
{
  "message": "Chess.com 요약 통계 조회 성공",
  "data": {
    "allGames": 3000,
    "ratedGames": 2800,
    "wins": 1400,
    "losses": 1000,
    "draws": 400
  }
}
```

---

#### `GET /api/stat/lichess-summary`

Lichess 전용 요약 통계 (rated 게임 기반).

> Chess.com은 해당 엔드포인트 없음.

**응답**
```json
{
  "message": "Lichess 요약 통계 조회 성공",
  "data": {
    "title": "GM",
    "lichessCreatedAt": "2015-03-01T00:00:00",
    "lastLoginAt": "2024-11-20T12:00:00",
    "allGames": 5000,
    "ratedGames": 4800,
    "wins": 2500,
    "losses": 1800,
    "draws": 500,
    "totalSeconds": 1200000
  }
}
```

---

## 7. 주요 변경 파일 목록

| 파일 | 변경 유형 | 설명 |
|------|-----------|------|
| `LichessUserEntity.java` | 수정 | 기본 정보만 남김, 통계 필드 제거 |
| `LichessUserStatEntity.java` | 신규 | `lichess_user_stat` 테이블 엔티티 |
| `LichessUserStat.java` | 신규 | 도메인 객체 |
| `LichessUserStatRepository.java` | 신규 | 도메인 Repository 인터페이스 |
| `LichessUserStatJpaRepository.java` | 신규 | JPA Repository |
| `LichessUserStatMapper.java` | 신규 | Entity ↔ Domain 변환 |
| `LichessUserStatRepositoryImpl.java` | 신규 | 구현체 |
| `LichessApi.java` | 수정 | `rated` 파라미터 추가 |
| `LichessGameSyncWorker.java` | 수정 | `rated=true` 전달 |
| `GameStatAggregator.java` | 수정 | rated 필터 + LichessUserStat 집계 |
| `StatService.java` | 신규(재작성) | 캐시 없는 직접 DB 조회 |
| `StatController.java` | 신규(재작성) | 4개 통계 엔드포인트 |
| `DailyGameStatResponse.java` | 신규 | DTO |
| `ColorStatResponse.java` | 신규 | DTO |
| `FirstMoveStatResponse.java` | 신규 | DTO |
| `LichessUserStatResponse.java` | 신규 | DTO |
| `UserService.java` | 신규(재작성) | 플랫폼 분기 프로필 조회/수정 |
| `UserController.java` | 신규(재작성) | 프로필, 자기소개 엔드포인트 |
| `ImageController.java` | 신규 | Presigned URL 업로드 흐름 |
| `ImageService.java` | 신규 | R2 key 관리, DB 저장 |
| `ImageUtil.java` | 수정 | CDN URL 빌드 (userId + key 기반) |
| `CloudflareR2Config.java` | 수정 | `@ConditionalOnProperty` 추가 |
