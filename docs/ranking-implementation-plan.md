# ChessMate 랭킹 시스템 구현 계획서

> 작성일: 2026-04-16  
> 작성자: nuza  
> 현재 브랜치: develop

---

## 목차

1. [현재 상태 분석](#1-현재-상태-분석)
2. [핵심 문제점 (주석 해제 전 반드시 수정)](#2-핵심-문제점)
3. [설계 결정사항](#3-설계-결정사항)
4. [구현 단계별 계획](#4-구현-단계별-계획)
5. [추가 개선 아이디어](#5-추가-개선-아이디어)
6. [API 명세](#6-api-명세)
7. [데이터 흐름도](#7-데이터-흐름도)
8. [구현 우선순위 요약](#8-구현-우선순위-요약)

---

## 1. 현재 상태 분석

### 1.1 파일별 구현 상태

| 계층 | 파일 | 상태 |
|------|------|------|
| Controller | `RankController.java` | 주석 처리됨 |
| Service | `RankService.java` | 주석 처리됨 (로직 오류 포함) |
| DTO | `RankingResponse`, `MyRankInfo`, `RankerDto` | 완전 구현 |
| Domain | `UserPerfStat`, `UserPerfStatRepository` | 완전 구현 (메서드 추가 필요) |
| Persistence | `UserPerfStatJpaRepository`, `UserPerfStatRepositoryImpl` | 구현됨 (메서드 추가 필요) |
| Cache | `CacheService.java` | **의도적으로 주석 처리** (추후 추가 예정) |
| 배치 | `GameStatAggregator`, `ScheduledSyncTrigger` | 정상 동작 |

### 1.2 도메인 모델 현황

```
UserPerfStat
├── id
├── userId      ← ChesscomUser.id 또는 LichessUser.id
├── platform    ← CHESSCOM | LICHESS
├── timeClass   ← "rapid" | "blitz" | "bullet" | "classical" (String)
├── rating
├── games       ← 총 게임 수
├── wins
├── losses
└── draws

GameType (enum)   ← RAPID | BLITZ | BULLET | CLASSICAL
UserPrincipal     ← id(Long) + provider(OAuthPlatForm)
```

**유저 정보**: 단일 `User` 클래스 없음 → `ChesscomUser` / `LichessUser` 별도 존재

---

## 2. 핵심 문제점

> **주석 처리된 코드를 그대로 살리면 컴파일 에러 발생. 아래 문제들을 먼저 수정해야 함.**

### 2.1 존재하지 않는 `UserPerf` 타입 참조

```java
// ❌ RankService (주석 처리 상태) - UserPerf 클래스 자체가 없음
List<UserPerf> allRankings = cacheService.getRanking(gameType);
UserPerf userPerf = userPerfRepository.findByUserIdAndGameType(...);

// ✅ 실제 도메인 타입으로 교체
List<UserPerfStat> allRankings = ...;
UserPerfStat myPerf = userPerfStatRepository.findByUserIdAndPlatformAndTimeClass(...);
```

### 2.2 존재하지 않는 Repository 메서드

```java
// ❌ 없는 메서드
userPerfRepository.findRankingByGameType(gameType)
userPerfRepository.findByUserIdAndGameType(userId, gameType)

// ✅ 새로 추가할 메서드
userPerfStatRepository.findRankingByPlatformAndTimeClassOrderByRatingDesc(platform, timeClass)
userPerfStatRepository.findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass)
```

### 2.3 존재하지 않는 `userPrincipal.getUser()`

```java
// ❌ RankController에서 - getUser() 메서드 없음
rankService.getRankers(userPrincipal.getUser(), ...)

// ✅ 수정
rankService.getRankers(userPrincipal.getId(), userPrincipal.getProvider(), ...)
```

### 2.4 `GameType` → `timeClass` 변환 없음

```java
// GameType은 enum(RAPID), UserPerfStat.timeClass는 String("rapid")
// ✅ 변환 유틸 추가 필요
GameType.RAPID → "rapid"
GameType.BLITZ → "blitz"
```

### 2.5 N+1 문제 (랭킹 목록 생성 시)

```java
// ❌ 현재: 페이지 내 유저마다 개별 DB 조회
for (int i = startIndex; i < endIndex; i++) {
    var rankingUser = userRepository.findById(ranking.getUserId()); // 매번 DB hit
}

// ✅ 개선: 페이지 내 userId 목록을 한 번에 bulk 조회
List<Long> pageUserIds = pageItems.stream().map(UserPerfStat::getUserId).toList();
Map<Long, ChesscomUser> userMap = chesscomUserRepository.findByIdIn(pageUserIds)
    .stream().collect(Collectors.toMap(ChesscomUser::getId, u -> u));
```

---

## 3. 설계 결정사항

### 3.1 랭킹 범위: 플랫폼별 분리 (필수 파라미터)

Chess.com과 Lichess는 레이팅 분포와 기준이 달라 같은 선상에서 비교 불가.  
→ `platform` 파라미터를 **필수**로 받아 플랫폼 내 랭킹만 조회.

```
GET /api/rank/ranking?gameType=RAPID&platform=LICHESS   → Lichess 내 Rapid 랭킹
GET /api/rank/ranking?gameType=BLITZ&platform=CHESSCOM  → Chess.com 내 Blitz 랭킹
```

### 3.2 Unrated 기준: 없음

- 별도 Unrated 판별 없음, **모든 유저가 랭킹에 포함**
- `MyRankInfo.unrated` 필드는 DTO에서 제거하거나 항상 `false` 반환

### 3.3 동점자 처리: id ASC

- 같은 rating이면 **userId가 작은 순서 (먼저 가입한 순)** 로 정렬
- `ORDER BY rating DESC, user_id ASC`
- 별도 컬럼 추가 없이 단순하게 처리

### 3.4 캐시 전략: Phase 1에서 제외

- `CacheService`는 의도적으로 주석 처리된 상태이며, **추후 추가 예정**
- Phase 1 (현재)은 캐시 없이 **DB 직접 조회**로 구현
- Phase 2에서 Redis Cache-Aside 또는 Sorted Set 도입

---

## 4. 구현 단계별 계획

### Phase 1: 핵심 기능 구현 (캐시 없이 DB 직접 조회)

#### Step 1-1. `GameType` 변환 유틸 추가

**파일**: `chessmate-common/.../type/GameType.java`

```java
public enum GameType {
    RAPID, BLITZ, CLASSICAL, BULLET;

    public String toTimeClass() {
        return this.name().toLowerCase();  // "rapid", "blitz" ...
    }

    public static GameType fromTimeClass(String timeClass) {
        return valueOf(timeClass.toUpperCase());
    }
}
```

#### Step 1-2. `UserPerfStatRepository` 메서드 추가

**파일**: `chessmate-domain/.../stat/UserPerfStatRepository.java`

```java
public interface UserPerfStatRepository {
    // 기존 메서드 유지...

    // 신규: 플랫폼 + timeClass 기준 랭킹 조회 (rating DESC, id ASC)
    List<UserPerfStat> findRankingByPlatformAndTimeClass(OAuthPlatForm platform, String timeClass);

    // 신규: 특정 유저의 특정 플랫폼 + 타임클래스 성능 조회
    Optional<UserPerfStat> findByUserIdAndPlatformAndTimeClass(
        Long userId, OAuthPlatForm platform, String timeClass);

    // 신규: 여러 userId bulk 조회 (N+1 방지)
    List<UserPerfStat> findByUserIdsAndPlatformAndTimeClass(
        List<Long> userIds, OAuthPlatForm platform, String timeClass);
}
```

#### Step 1-3. JPA Repository 메서드 추가

**파일**: `chessmate-infra-persistence/.../UserPerfStatJpaRepository.java`

```java
// 랭킹 조회: 동점자는 userId ASC
@Query("SELECT s FROM UserPerfStatJpaEntity s " +
       "WHERE s.platform = :platform AND s.timeClass = :timeClass " +
       "ORDER BY s.rating DESC, s.userId ASC")
List<UserPerfStatJpaEntity> findRankingByPlatformAndTimeClass(
    @Param("platform") OAuthPlatForm platform,
    @Param("timeClass") String timeClass);

// 단건 조회
Optional<UserPerfStatJpaEntity> findByUserIdAndPlatformAndTimeClass(
    Long userId, OAuthPlatForm platform, String timeClass);

// Bulk 조회 (N+1 방지)
@Query("SELECT s FROM UserPerfStatJpaEntity s " +
       "WHERE s.userId IN :userIds " +
       "AND s.platform = :platform AND s.timeClass = :timeClass")
List<UserPerfStatJpaEntity> findByUserIdInAndPlatformAndTimeClass(
    @Param("userIds") List<Long> userIds,
    @Param("platform") OAuthPlatForm platform,
    @Param("timeClass") String timeClass);
```

#### Step 1-4. `UserPerfStatRepositoryImpl` 구현

**파일**: `chessmate-infra-persistence/.../UserPerfStatRepositoryImpl.java`

```java
@Override
public List<UserPerfStat> findRankingByPlatformAndTimeClass(
        OAuthPlatForm platform, String timeClass) {
    return jpaRepository.findRankingByPlatformAndTimeClass(platform, timeClass)
        .stream().map(mapper::toDomain).toList();
}

@Override
public Optional<UserPerfStat> findByUserIdAndPlatformAndTimeClass(
        Long userId, OAuthPlatForm platform, String timeClass) {
    return jpaRepository.findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass)
        .map(mapper::toDomain);
}
```

#### Step 1-5. `ChesscomUserRepository` / `LichessUserRepository` bulk 조회 추가

N+1 방지를 위해 페이지 내 유저 정보를 한 번에 조회.

```java
// ChesscomUserRepository
List<ChesscomUser> findByIdIn(List<Long> ids);

// LichessUserRepository
List<LichessUser> findByIdIn(List<Long> ids);
```

#### Step 1-6. `MyRankInfo` DTO 수정

`unrated` 필드 제거 (또는 deprecated 처리).

```java
// ✅ unrated 필드 제거 후
public class MyRankInfo {
    private boolean loggedInUser;
    private int rank;
    private int rating;
    private Long userId;
    private String username;
    private String banner;
    private String profile;
    private String description;

    public static MyRankInfo notLoggedIn() {
        return new MyRankInfo(false, 0, 0, null, null, null, null, null);
    }
}
```

#### Step 1-7. `RankService` 재작성

**파일**: `chessmate-api/.../rank/service/RankService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

    private final UserPerfStatRepository userPerfStatRepository;
    private final LichessUserRepository lichessUserRepository;
    private final ChesscomUserRepository chesscomUserRepository;
    private final ImageUtil imageUtil;

    @Transactional(readOnly = true)
    public RankingResponse getRankers(Long userId, OAuthPlatForm platform,
                                      GameType gameType, Pageable pageable) {
        // 비로그인 처리
        if (userId == null || platform == null) {
            return buildGuestResponse(pageable);
        }

        String timeClass = gameType.toTimeClass();

        // 1. 전체 랭킹 DB 직접 조회 (캐시 없음 - 추후 추가 예정)
        List<UserPerfStat> allRankings =
            userPerfStatRepository.findRankingByPlatformAndTimeClass(platform, timeClass);

        // 2. 내 퍼프 조회
        UserPerfStat myPerf = userPerfStatRepository
            .findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass)
            .orElse(null);

        // 3. 내 순위 계산
        MyRankInfo myRankInfo = buildMyRankInfo(userId, platform, myPerf, allRankings);

        // 4. 페이지네이션
        int total = allRankings.size();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startIndex = (int) pageable.getOffset();
        int endIndex = Math.min(startIndex + pageSize, total);

        if (startIndex >= total && total > 0) {
            return buildEmptyPageResponse(myRankInfo, total, currentPage, pageSize);
        }

        // 5. 페이지 내 유저 정보 bulk 조회 (N+1 방지)
        List<UserPerfStat> pageItems = allRankings.subList(startIndex, endIndex);
        List<Long> pageUserIds = pageItems.stream().map(UserPerfStat::getUserId).toList();
        Map<Long, String>  usernameMap  = loadUsernameMap(pageUserIds, platform);
        Map<Long, String>  bannerMap    = loadBannerMap(pageUserIds, platform);
        Map<Long, String>  profileMap   = loadProfileMap(pageUserIds, platform);

        // 6. RankerDto 조립
        List<RankerDto> rankers = new ArrayList<>();
        for (int i = 0; i < pageItems.size(); i++) {
            UserPerfStat stat = pageItems.get(i);
            Long uid = stat.getUserId();
            rankers.add(RankerDto.builder()
                .userId(uid)
                .username(usernameMap.getOrDefault(uid, "Unknown"))
                .rating(stat.getRating())
                .rank(startIndex + i + 1)
                .bannerImage(bannerMap.get(uid))
                .profileImage(profileMap.get(uid))
                .build());
        }

        return RankingResponse.builder()
            .myRankInfo(myRankInfo)
            .ranking(rankers)
            .totalCount(total)
            .currentPage(currentPage)
            .pageSize(pageSize)
            .totalPages((long) Math.ceil((double) total / pageSize))
            .build();
    }

    private MyRankInfo buildMyRankInfo(Long userId, OAuthPlatForm platform,
                                       UserPerfStat myPerf, List<UserPerfStat> allRankings) {
        if (myPerf == null) {
            return MyRankInfo.notLoggedIn();
        }

        // 내 순위: 나보다 rating 높은 사람 수 + 1
        // 동점자는 DB ORDER BY 기준(rating DESC, userId ASC) 그대로 반영
        int myRank = (int) allRankings.stream()
            .takeWhile(r -> r.getRating() > myPerf.getRating()
                || (r.getRating() == myPerf.getRating() && r.getUserId() < userId))
            .count() + 1;

        // 유저 프로필 정보 조회 (단건 - 내 정보만)
        String username = loadUsername(userId, platform);
        String banner   = loadBanner(userId, platform);
        String profile  = loadProfile(userId, platform);

        return MyRankInfo.builder()
            .loggedInUser(true)
            .rank(myRank)
            .rating(myPerf.getRating())
            .userId(userId)
            .username(username)
            .banner(banner)
            .profile(profile)
            .build();
    }
}
```

#### Step 1-8. `RankController` 복원 및 수정

**파일**: `chessmate-api/.../rank/controller/RankController.java`

`platform` 파라미터를 **필수**로 추가.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rank")
@Slf4j
public class RankController {

    private final RankService rankService;

    @GetMapping("/ranking")
    public ResponseEntity<SuccessResponse<RankingResponse>> getRanking(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(defaultValue = "RAPID") GameType gameType,
        @RequestParam OAuthPlatForm platform,           // 플랫폼 필수
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;

        RankingResponse response = rankService.getRankers(userId, platform, gameType, pageable);
        return ResponseEntity.ok(new SuccessResponse<>("Ranking 조회 성공", response));
    }
}
```

---

### Phase 2: Redis 캐시 도입 (추후)

> `CacheService`는 현재 의도적으로 비활성화된 상태. Phase 1 완료 후 추가.

#### Option A: Cache-Aside 패턴 (단순, 먼저 시도)

```
요청 → Redis 조회 → HIT: 반환
                  → MISS: DB 조회 → Redis 저장(TTL 1h) → 반환
```

캐시 키: `rank:ranking:{PLATFORM}:{GAME_TYPE}`  
예: `rank:ranking:LICHESS:RAPID`

```java
// CacheService 복원 후 적용
List<UserPerfStat> allRankings = cacheService.getRanking(platform, gameType);
if (allRankings == null) {
    allRankings = userPerfStatRepository.findRankingByPlatformAndTimeClass(...);
    cacheService.saveRanking(platform, gameType, allRankings);
}
```

#### Option B: Redis Sorted Set (성능 최적화)

Cache-Aside보다 구조적으로 우수. 내 순위 조회가 O(n) → O(log n).

```java
// 저장: ZADD rank:LICHESS:RAPID {userId} {rating}
redisTemplate.opsForZSet().add(key, userId.toString(), rating);

// 내 순위: ZREVRANK → O(log n)
Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());

// 페이지 조회: ZREVRANGE → O(log n + pageSize)
Set<ZSetOperations.TypedTuple<Object>> page =
    redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
```

| 구분 | Cache-Aside (List) | Sorted Set |
|------|-------------------|------------|
| 역직렬화 | 복잡 (LinkedHashMap 처리) | 단순 (userId만 저장) |
| 내 순위 | O(n) | O(log n) |
| 페이지 | O(1) subList | O(log n + k) |
| 구현 난이도 | 낮음 | 중간 |

→ **Phase 2-a**: Cache-Aside 먼저 도입 → **Phase 2-b**: Sorted Set으로 전환

#### 배치 완료 후 캐시 갱신 연결

```java
// GameStatAggregator.aggregate() 완료 시점에 추가
// 무효화 방식 (다음 요청 시 DB 재조회)
cacheService.deleteRanking(platform, gameType);

// 또는 사전 갱신 방식 (Cache-Miss 제거)
List<UserPerfStat> fresh = userPerfStatRepository.findRankingByPlatformAndTimeClass(...);
cacheService.saveRanking(platform, gameType, fresh);
```

---

### Phase 3: 랭킹 고도화 (장기)

#### Step 3-1. DB 인덱스 최적화

```sql
-- 랭킹 쿼리 전용 복합 인덱스
ALTER TABLE user_perf_stat
ADD INDEX idx_perf_ranking (platform, time_class, rating DESC, user_id ASC);
-- WHERE platform = ? AND time_class = ? ORDER BY rating DESC, user_id ASC
```

#### Step 3-2. 랭킹 변동 표시 (▲▼)

신규 테이블 `rank_snapshot`:

```sql
CREATE TABLE rank_snapshot (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    platform    VARCHAR(20) NOT NULL,
    time_class  VARCHAR(20) NOT NULL,
    rank        INT NOT NULL,
    rating      INT NOT NULL,
    snapshot_at DATE NOT NULL,
    UNIQUE INDEX uk_snapshot (user_id, platform, time_class, snapshot_at)
);
```

매일 00:00 스냅샷 저장 → 전날 대비 순위 변동 표시.

응답 DTO 추가 필드:
```json
{
  "rank": 5,
  "rank_change": 3,
  "rank_direction": "UP"
}
```

#### Step 3-3. 내 주변 랭킹 API (Neighborhood)

내 순위 기준 위/아래 N명 반환:

```
GET /api/rank/neighborhood?gameType=RAPID&platform=LICHESS&range=5
```

Redis Sorted Set의 `ZREVRANGE(myRank - range, myRank + range)`로 O(log n) 구현.

#### Step 3-4. 캐시 Stampede 방지

사용자가 많아지면 Cache-Miss 동시 발생으로 DB 폭주 가능.  
Redis 분산 락으로 캐시 갱신 직렬화:

```java
String lockKey = "lock:rank:" + platform + ":" + gameType;
if (redisService.setIfAbsent(lockKey, "1", Duration.ofSeconds(30))) {
    try {
        // DB 조회 + 캐시 갱신
    } finally {
        redisService.delete(lockKey);
    }
} else {
    // 락 획득 실패 → 잠시 대기 후 캐시 재조회
}
```

---

## 5. 추가 개선 아이디어

### 💡 아이디어 1: 상위 N% 배지 표시

전체 랭킹에서 상위 1%, 10% 해당 유저에게 배지 부여.

```java
// RankerDto에 추가
private String tier;  // "TOP_1" | "TOP_10" | null

int total = allRankings.size();
String tier = null;
if (rank <= total * 0.01) tier = "TOP_1";
else if (rank <= total * 0.10) tier = "TOP_10";
```

### 💡 아이디어 2: 국가별 랭킹

`ChesscomUser` / `LichessUser`에 `country` 필드 추가 후 필터 지원:

```
GET /api/rank/ranking?gameType=RAPID&platform=LICHESS&country=KR
```

### 💡 아이디어 3: 레이팅 이력 그래프 연결

현재 `MonthlyRatingProjection`으로 월별 레이팅 이력이 이미 구현됨.  
랭킹 프로필 페이지에서 바로 연결 가능:

```
GET /api/stat/{userId}/rating-history?platform=LICHESS&gameType=RAPID
```

### 💡 아이디어 4: 게임 수 표시 (정보성)

Unrated 판별은 없지만, 게임 수가 적은 유저의 레이팅은 신뢰도가 낮을 수 있음.  
정보 제공 목적으로 `games` 필드를 응답에 포함:

```json
{
  "rating": 1850,
  "games": 12
}
```

---

## 6. API 명세

### `GET /api/rank/ranking`

**설명**: 플랫폼 + 게임 타입별 랭킹 조회

**파라미터**:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `gameType` | `GameType` | N | `RAPID` | `RAPID` \| `BLITZ` \| `BULLET` \| `CLASSICAL` |
| `platform` | `OAuthPlatForm` | **Y** | - | `LICHESS` \| `CHESSCOM` |
| `page` | `int` | N | `0` | 페이지 번호 (0-indexed) |
| `size` | `int` | N | `20` | 페이지 크기 |

**응답 예시 (로그인)**:

```json
{
  "message": "Ranking 조회 성공",
  "data": {
    "my_rank_info": {
      "logged_in_user": true,
      "rank": 42,
      "rating": 1580,
      "user_id": 7,
      "username": "nuza",
      "banner": "https://...",
      "profile": "https://...",
      "description": "Chess lover"
    },
    "ranking": [
      {
        "user_id": 1,
        "username": "GrandMaster_Kim",
        "rating": 2400,
        "rank": 1,
        "banner_image": "https://...",
        "profile_image": "https://...",
        "description": ""
      }
    ],
    "total_count": 1250,
    "current_page": 0,
    "page_size": 20,
    "total_pages": 63
  }
}
```

**응답 예시 (비로그인)**:

```json
{
  "data": {
    "my_rank_info": {
      "logged_in_user": false,
      "rank": 0,
      "rating": 0
    },
    "ranking": [],
    "total_count": 0
  }
}
```

---

## 7. 데이터 흐름도

```
[클라이언트]
    │
    ▼
GET /api/rank/ranking?gameType=RAPID&platform=LICHESS&page=0
    │
    ▼
[RankController]
  ├─ JWT 인증 → UserPrincipal (id, provider)
  └─ RankService.getRankers(userId, platform=LICHESS, RAPID, pageable)
      │
      ├─ [Phase 1] DB 직접 조회 ─────────────────────────────────┐
      │   userPerfStatRepository                                  │
      │   .findRankingByPlatformAndTimeClass(LICHESS, "rapid")    │
      │   ORDER BY rating DESC, user_id ASC                       │
      │                                                           │
      ├─ [Phase 2 예정] Redis Cache-Aside ────────────────────────┘
      │   rank:ranking:LICHESS:RAPID
      │
      ├─ 내 UserPerfStat 조회 (userId + LICHESS + "rapid")
      │
      ├─ 내 순위 계산
      │   stream.takeWhile(rating > mine || (rating == mine && id < mine)).count() + 1
      │
      ├─ 페이지 내 userId → LichessUserRepository.findByIdIn(ids) bulk 조회
      │
      └─ RankingResponse 조립 → 반환

[배치 스케줄러 (30분 간격)]
    │
    ▼
GameStatAggregator.aggregate()
    │
    └─ [Phase 2 예정] 집계 완료 후 캐시 무효화/갱신
```

---

## 8. 구현 우선순위 요약

| 우선순위 | 작업 | 비고 |
|---------|------|------|
| 🔴 P0 | `GameType.toTimeClass()` 추가 | enum 메서드 2줄 |
| 🔴 P0 | Repository 메서드 3개 추가 (Domain + JPA + Impl) | platform 포함 쿼리 |
| 🔴 P0 | `MyRankInfo.unrated` 필드 제거 | DTO 단순화 |
| 🔴 P0 | `RankService` 재작성 | 캐시 없이 DB 직접 조회 |
| 🔴 P0 | `RankController` 복원 + platform 파라미터 추가 | 필수 파라미터 |
| 🟡 P1 | `findByIdIn()` bulk 조회 추가 (Chesscom/Lichess) | N+1 방지 |
| 🟡 P1 | DB 복합 인덱스 추가 | `(platform, time_class, rating DESC, user_id ASC)` |
| 🟢 P2 | `CacheService` 복원 + Cache-Aside 적용 | 추후 추가 예정 |
| 🟢 P2 | Redis Sorted Set으로 전환 | Cache-Aside 이후 |
| 🟢 P2 | 배치 완료 후 캐시 갱신 연결 | Phase 2 캐시 도입 후 |
| 🔵 P3 | 랭킹 변동 표시 (`rank_snapshot`) | 신규 테이블 필요 |
| 🔵 P3 | 내 주변 랭킹 API (Neighborhood) | Redis Sorted Set 전제 |
| 🔵 P3 | 캐시 Stampede 방지 (분산 락) | 트래픽 증가 시 |