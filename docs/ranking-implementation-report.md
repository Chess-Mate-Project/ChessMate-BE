# 랭킹 시스템 구현 보고서

> 작성일: 2026-04-16  
> 브랜치: develop

---

## 1. 변경된 파일 목록

| 모듈 | 파일 | 변경 유형 |
|------|------|----------|
| `chessmate-common` | `GameType.java` | 수정 |
| `chessmate-domain` | `UserPerfStatRepository.java` | 수정 |
| `chessmate-domain` | `LichessUserRepository.java` | 수정 |
| `chessmate-domain` | `ChesscomUserRepository.java` | 수정 |
| `chessmate-infra-persistence` | `UserPerfStatJpaRepository.java` | 수정 |
| `chessmate-infra-persistence` | `UserPerfStatRepositoryImpl.java` | 수정 |
| `chessmate-infra-persistence` | `LichessUserRepositoryImpl.java` | 수정 |
| `chessmate-infra-persistence` | `ChesscomUserRepositoryImpl.java` | 수정 |
| `chessmate-api` | `MyRankInfo.java` | 수정 |
| `chessmate-api` | `RankService.java` | 전면 재작성 |
| `chessmate-api` | `RankController.java` | 전면 재작성 |

---

## 2. 전체 요청 흐름

```
클라이언트
    │
    ▼  GET /api/rank/ranking?platform=LICHESS&gameType=RAPID&page=0&size=20
    │  Authorization: Bearer {JWT}
    │
    ▼
[RankController]
  ├─ JWT 파싱 → UserPrincipal (id, provider)
  ├─ @RequestParam platform (필수)
  └─ rankService.getRankers(userId, platform, gameType, pageable)
        │
        ├─ [비로그인] → buildGuestResponse() → 빈 응답 반환
        │
        ├─ [로그인] gameType.toTimeClass() → "rapid"
        │
        ├─ ① DB 쿼리: 전체 랭킹 조회
        │    userPerfStatRepository.findRankingByPlatformAndTimeClass(LICHESS, "rapid")
        │    → SELECT * FROM user_perf_stat
        │      WHERE platform = 'LICHESS' AND time_class = 'rapid'
        │      ORDER BY rating DESC, user_id ASC
        │
        ├─ ② DB 쿼리: 내 성능 단건 조회
        │    userPerfStatRepository.findByUserIdAndPlatformAndTimeClass(userId, LICHESS, "rapid")
        │    → SELECT * FROM user_perf_stat
        │      WHERE user_id = ? AND platform = 'LICHESS' AND time_class = 'rapid'
        │
        ├─ ③ 내 순위 계산
        │    전체 목록을 순회해 내 userId 위치 탐색 → rank = index + 1
        │    (게임 이력 없으면 rank = 0)
        │
        ├─ ④ 내 유저 정보 단건 조회
        │    lichessUserRepository.findById(userId)
        │    → SELECT * FROM lichess_user WHERE id = ?
        │
        ├─ ⑤ 페이지 슬라이싱
        │    allRankings.subList(startIndex, endIndex)
        │
        ├─ ⑥ DB 쿼리: 페이지 내 유저 정보 bulk 조회 (N+1 방지)
        │    lichessUserRepository.findByIdIn(pageUserIds)
        │    → SELECT * FROM lichess_user WHERE id IN (?, ?, ...)
        │
        └─ ⑦ RankingResponse 조립 → 반환
```

---

## 3. 계층별 구현 상세

### 3.1 Controller 계층

**파일**: `chessmate-api/.../rank/controller/RankController.java`

```java
@GetMapping("/ranking")
public ResponseEntity<SuccessResponse<RankingResponse>> getRanking(
    @AuthenticationPrincipal UserPrincipal userPrincipal,
    @RequestParam(defaultValue = "RAPID") GameType gameType,
    @RequestParam OAuthPlatForm platform,           // 필수
    @PageableDefault(size = 20) Pageable pageable
) {
    Long userId = userPrincipal != null ? userPrincipal.getId() : null;
    RankingResponse response = rankService.getRankers(userId, platform, gameType, pageable);
    return ResponseEntity.ok(new SuccessResponse<>("Ranking 조회 성공", response));
}
```

**설계 포인트**:
- `platform`은 **필수** 파라미터 (`@RequestParam`, defaultValue 없음)
  - Chess.com과 Lichess는 레이팅 체계가 달라 통합 랭킹이 의미 없음
- `UserPrincipal`에서 `getUser()` 대신 `getId()` 직접 사용
  - 기존 주석 코드는 존재하지 않는 `getUser()` 메서드를 호출하고 있었음
- 비로그인 사용자는 `userId = null`로 전달 → Service에서 guest 응답 처리

---

### 3.2 Service 계층

**파일**: `chessmate-api/.../rank/service/RankService.java`

#### 의존성

| 의존 | 용도 |
|------|------|
| `UserPerfStatRepository` | 랭킹 데이터 조회 |
| `LichessUserRepository` | Lichess 유저 프로필 조회 |
| `ChesscomUserRepository` | Chess.com 유저 프로필 조회 |
| `ImageUtil` | 이미지 URL 생성 (CDN 기반) |

#### 내 순위 계산 방식

전체 랭킹 리스트는 DB에서 `rating DESC, user_id ASC` 정렬로 가져옴.  
리스트를 순회해 내 `userId`가 있는 위치를 찾은 뒤 `rank = index + 1`.

```java
int myRank = 0;
for (int i = 0; i < allRankings.size(); i++) {
    if (allRankings.get(i).getUserId().equals(userId)) {
        myRank = i + 1;
        break;
    }
}
```

- 게임 이력이 없어 `UserPerfStat`이 없는 경우 → `rank = 0`, `rating = 0`
- 동점자 처리는 DB 정렬(`user_id ASC`)과 일치하므로 별도 로직 불필요

#### N+1 문제 해결

기존 주석 코드의 문제점:
```java
// ❌ 페이지 크기만큼 쿼리 발생 (20 페이지 = 20번 DB 호출)
for (int i = startIndex; i < endIndex; i++) {
    var rankingUser = userRepository.findById(ranking.getUserId());
}
```

개선된 방식:
```java
// ✅ 한 번의 IN 쿼리로 페이지 전체 유저 정보 조회
List<Long> pageUserIds = pageItems.stream().map(UserPerfStat::getUserId).toList();
Map<Long, LichessUser> userMap = lichessUserRepository.findByIdIn(pageUserIds)
    .stream().collect(Collectors.toMap(LichessUser::getId, u -> u));
// → SELECT * FROM lichess_user WHERE id IN (?, ?, ...)  — 쿼리 1회
```

#### 캐시 없음 (의도적)

현재 `CacheService`는 전면 비활성화 상태. 추후 Redis Cache-Aside 또는 Sorted Set 패턴으로 추가 예정.  
현재는 모든 조회가 DB 직접 쿼리.

---

### 3.3 Domain Repository 계층

**파일**: `chessmate-domain/.../stat/UserPerfStatRepository.java`

#### 추가된 메서드 2개

```java
// 1. 플랫폼 + 게임타입 기준 전체 랭킹 조회
List<UserPerfStat> findRankingByPlatformAndTimeClass(OAuthPlatForm platform, String timeClass);

// 2. 특정 유저의 특정 플랫폼 + 게임타입 성능 단건 조회
Optional<UserPerfStat> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass);
```

**파일**: `chessmate-domain/.../lichess/user/LichessUserRepository.java`  
**파일**: `chessmate-domain/.../chesscom/user/ChesscomUserRepository.java`

```java
// N+1 방지용 bulk 조회
List<LichessUser> findByIdIn(List<Long> ids);
List<ChesscomUser> findByIdIn(List<Long> ids);
```

---

### 3.4 JPA Repository 계층

**파일**: `chessmate-infra-persistence/.../UserPerfStatJpaRepository.java`

#### 신규 쿼리 1: 전체 랭킹 조회

```java
@Query("SELECT s FROM UserPerfStatJpaEntity s " +
       "WHERE s.platform = :platform AND s.timeClass = :timeClass " +
       "ORDER BY s.rating DESC, s.userId ASC")
List<UserPerfStatJpaEntity> findRankingByPlatformAndTimeClass(
    @Param("platform") OAuthPlatForm platform,
    @Param("timeClass") String timeClass
);
```

**SQL 변환**:
```sql
SELECT *
FROM user_perf_stat
WHERE platform = 'LICHESS'
  AND time_class = 'rapid'
ORDER BY rating DESC, user_id ASC;
```

**설명**:
- `platform` 조건: Chess.com / Lichess 레이팅 체계가 다르므로 플랫폼별 분리
- `time_class` 조건: 게임 타입(rapid, blitz, bullet, classical) 필터
- `ORDER BY rating DESC`: 레이팅 높은 순으로 정렬
- `ORDER BY user_id ASC`: 동점자는 userId가 작은 순(먼저 가입한 순)으로 결정

#### 신규 쿼리 2: 특정 유저 단건 조회 (Derived Query)

```java
Optional<UserPerfStatJpaEntity> findByUserIdAndPlatformAndTimeClass(
    Long userId, OAuthPlatForm platform, String timeClass
);
```

**SQL 변환**:
```sql
SELECT *
FROM user_perf_stat
WHERE user_id = ?
  AND platform = ?
  AND time_class = ?
LIMIT 1;
```

**설명**:
- Spring Data JPA Derived Query — 메서드명에서 자동으로 쿼리 생성
- `user_id + platform + time_class`는 UNIQUE 제약으로 결과는 항상 0건 또는 1건
- 내 현재 레이팅을 가져오는 용도

#### 기존 쿼리: bulk 유저 정보 조회 (LichessUserJpaRepository)

```java
// JpaRepository 기본 제공 메서드 활용
List<LichessUserEntity> findAllById(Iterable<Long> ids);
```

**SQL 변환**:
```sql
SELECT *
FROM lichess_user
WHERE id IN (1, 2, 3, ...);
```

**설명**:
- `JpaRepository`가 기본 제공하는 `findAllById()` 사용
- Impl에서 `jpaRepository.findAllById(ids)` 호출로 N+1 해결

---

### 3.5 DTO 계층

#### `MyRankInfo` — 내 순위 정보

```java
public class MyRankInfo {
    private boolean loggedInUser; // 로그인 여부
    private int rank;             // 순위 (0 = 게임 이력 없음)
    private int rating;           // 현재 레이팅
    private Long userId;
    private String username;
    private String banner;        // CDN URL
    private String profile;       // CDN URL
    private String description;
}
```

변경 사항: `unrated` 필드 제거 (모든 유저 랭킹 포함, Unrated 판별 없음)

#### `RankerDto` — 랭킹 목록 한 줄

```java
public class RankerDto {
    private Long userId;
    private String username;
    private String description;
    private int rating;
    private int rank;
    private String bannerImage;   // CDN URL
    private String profileImage;  // CDN URL
}
```

#### `RankingResponse` — 전체 응답

```java
public class RankingResponse {
    private MyRankInfo myRankInfo;    // 요청자 본인 순위 정보
    private List<RankerDto> ranking;  // 해당 페이지의 랭커 목록
    private long totalCount;          // 전체 랭커 수
    private int currentPage;          // 현재 페이지 (0-indexed)
    private int pageSize;             // 페이지 크기
    private long totalPages;          // 전체 페이지 수
}
```

---

### 3.6 공통 모듈 변경

#### `GameType` — timeClass 변환 유틸 추가

```java
public enum GameType {
    RAPID, BLITZ, CLASSICAL, BULLET;

    public String toTimeClass() {
        return this.name().toLowerCase(); // "rapid", "blitz", "classical", "bullet"
    }

    public static GameType fromTimeClass(String timeClass) {
        return valueOf(timeClass.toUpperCase());
    }
}
```

`UserPerfStat.timeClass`가 소문자 String(`"rapid"`)이고, `GameType`은 대문자 enum(`RAPID`)이므로 변환이 필요함.

---

## 4. DB 쿼리 전체 목록

랭킹 API 요청 1건당 발생하는 DB 쿼리:

| 순서 | 쿼리 | 발생 횟수 | 목적 |
|------|------|----------|------|
| 1 | `SELECT * FROM user_perf_stat WHERE platform=? AND time_class=? ORDER BY rating DESC, user_id ASC` | 1회 | 전체 랭킹 조회 |
| 2 | `SELECT * FROM user_perf_stat WHERE user_id=? AND platform=? AND time_class=?` | 1회 | 내 성능 단건 조회 |
| 3 | `SELECT * FROM lichess_user WHERE id=?` | 1회 | 내 프로필 정보 조회 |
| 4 | `SELECT * FROM lichess_user WHERE id IN (?,?,...)` | 1회 | 페이지 내 유저 정보 bulk 조회 |

**총 4회** (페이지 크기와 무관하게 고정)

---

## 5. 설계 결정 근거

### 5.1 platform을 필수 파라미터로

Chess.com과 Lichess의 레이팅 체계가 다름:
- Lichess 레이팅은 평균적으로 Chess.com보다 200~300점 높게 분포
- 같은 기준으로 비교하면 Lichess 유저가 Chess.com 유저를 압도
- → 플랫폼별로 독립된 랭킹을 운영

### 5.2 동점자 처리: `userId ASC`

- 별도 컬럼(created_at 등) 없이 단순 처리
- 먼저 가입한 사람이 유리한 합리적인 기준

### 5.3 캐시 없이 DB 직접 조회

- `CacheService`는 전면 비활성화 상태 (추후 추가 예정)
- 현재 사용자 규모에서는 DB 직접 조회로 충분
- 추후 트래픽 증가 시 Redis Cache-Aside 또는 Sorted Set 도입 예정

---

## 6. 기존 코드의 문제점 및 수정 내역

| 문제 | 기존 (주석 코드) | 수정 |
|------|----------------|------|
| 없는 타입 참조 | `List<UserPerf>` | `List<UserPerfStat>` |
| 없는 메서드 | `userPerfRepository.findRankingByGameType()` | `findRankingByPlatformAndTimeClass()` |
| 없는 메서드 | `userPerfRepository.findByUserIdAndGameType()` | `findByUserIdAndPlatformAndTimeClass()` |
| 없는 메서드 | `userPrincipal.getUser()` | `userPrincipal.getId()` |
| 타입 불일치 | `GameType` enum → `timeClass` String 변환 없음 | `GameType.toTimeClass()` 추가 |
| N+1 | 루프 안에서 `findById()` 반복 | `findByIdIn()` bulk 조회 |
| 제거된 필드 | `MyRankInfo.unrated` (Unrated 기준 삭제) | 필드 제거 |
| 잘못된 랭킹 범위 | 플랫폼 구분 없이 통합 | platform 필수 파라미터로 분리 |