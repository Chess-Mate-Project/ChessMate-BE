# Bug: Chess.com 신규 유저 집계 데이터 누락

## 현상

- 어제 가입한 일부 Chess.com 유저의 집계 테이블(`user_daily_game_stat` 등)이 비어 있음
- 실제 게임 데이터는 `game` 테이블에 존재하고, worker 수집 로그도 정상적으로 찍힘
- Lichess 유저는 동일 현상 없음

---

## 원인

### Chess.com 게임의 `platformGameId`는 양쪽 플레이어가 공유하는 URL이다

Chess.com API가 반환하는 게임 URL(`https://www.chess.com/game/live/12345`)은
해당 게임에 참여한 두 플레이어 **모두의 아카이브에 동일하게 등장**한다.

### 기존 중복 체크가 `userId`를 고려하지 않았다

`GameRepositoryImpl.saveAll()`은 게임 저장 전에 DB에서 이미 존재하는 `platformGameId`를 조회해 중복을 걸러낸다.
이 쿼리가 `platform`만 조건으로 사용하고 `userId`를 포함하지 않았다.

```java
// 버그: userId 없이 platform 전체에서 중복 검색
SELECT g.platformGameId FROM GameJpaEntity g
WHERE g.platform = :platform
  AND g.platformGameId IN :ids
```

DB의 unique constraint도 `(platform, platform_game_id)`으로만 걸려 있어서
같은 게임을 두 플레이어가 각자의 레코드로 저장하는 구조 자체가 막혀 있었다.

### 시나리오

| 순서 | 상황 | 결과 |
|------|------|------|
| 1 | 유저 A 가입 → 전체 수집 실행 | 게임 URL `12345`가 `userId=A`로 저장됨 |
| 2 | 유저 B 가입 (A와 대국 이력 있음) → 전체 수집 실행 | `findExistingPlatformGameIds`가 URL `12345`를 이미 존재로 판단 → **B의 해당 게임 저장 스킵** |
| 3 | B의 수집 결과 `newlySaved = 0`, stat 테이블도 비어있지 않다고 판단 | **집계(`aggregate`) 미실행** |
| 4 | 이후 30분 스케줄러 반복 실행 | B의 최근 job이 `COMPLETED`이므로 cursor=전월 → 당월만 재수집 → 여전히 신규 게임 없음 → 집계 영구 미실행 |

---

## 수정 내용

> **주의**: 아래 코드/스키마 변경은 이 PR에 포함되지 않으며 후속 PR에서 반영 예정입니다.

### 1. `GameJpaEntity.java` — unique constraint에 `user_id` 추가

**파일**: `chessmate-infra-persistence/.../game/entity/GameJpaEntity.java`

같은 게임 URL이라도 플레이어가 다르면 별도 레코드로 저장돼야 한다.
고유성 기준을 `(platform, platform_game_id)`에서 `(user_id, platform, platform_game_id)`으로 변경.

```java
// Before
@UniqueConstraint(name = "uk_game_platform_id", columnNames = {"platform", "platform_game_id"})

// After
@UniqueConstraint(name = "uk_game_user_platform_id", columnNames = {"user_id", "platform", "platform_game_id"})
```

### 2. `GameJpaRepository.java` — 중복 체크 쿼리에 `userId` 조건 추가

**파일**: `chessmate-infra-persistence/.../game/jpaRepository/GameJpaRepository.java`

중복 확인 쿼리를 "이 유저+플랫폼에서 이미 저장된 게임인지"로 범위를 좁힌다.
`userId` 없이 platform 전체를 뒤지면 다른 유저의 게임과 충돌한다.

```java
// Before
@Query("SELECT g.platformGameId FROM GameJpaEntity g
        WHERE g.platform = :platform AND g.platformGameId IN :ids")
List<String> findExistingPlatformGameIds(
    @Param("platform") OAuthPlatForm platform,
    @Param("ids") List<String> ids
);

// After
@Query("SELECT g.platformGameId FROM GameJpaEntity g
        WHERE g.userId = :userId AND g.platform = :platform AND g.platformGameId IN :ids")
List<String> findExistingPlatformGameIds(
    @Param("userId") Long userId,
    @Param("platform") OAuthPlatForm platform,
    @Param("ids") List<String> ids
);
```

### 3. `GameRepositoryImpl.java` — `userId` 파라미터 전달

**파일**: `chessmate-infra-persistence/.../game/repositoryImpl/GameRepositoryImpl.java`

`saveAll()`에서 `userId`를 꺼내 중복 체크 쿼리에 넘긴다.

```java
// Before
var platform = games.get(0).getPlatform();
Set<String> existing = Set.copyOf(
    jpaRepository.findExistingPlatformGameIds(platform, requestedIds)
);

// After
var userId = games.get(0).getUserId();
var platform = games.get(0).getPlatform();
Set<String> existing = Set.copyOf(
    jpaRepository.findExistingPlatformGameIds(userId, platform, requestedIds)
);
```

---

## DB 작업 (배포 전 실행)

프로젝트가 `ddl-auto: update`를 사용하므로 Hibernate는 기존 constraint를 자동으로 **삭제하지 않는다**.
코드 배포 전에 아래 SQL을 직접 실행해야 한다.

```sql
-- 기존 constraint 제거 후 user_id 포함 constraint 추가
ALTER TABLE game
  DROP INDEX uk_game_platform_id,
  ADD CONSTRAINT uk_game_user_platform_id UNIQUE (user_id, platform, platform_game_id);
```

> 이 작업을 **배포 전**에 해야 하는 이유:
> 코드 배포 후 Hibernate가 `uk_game_user_platform_id`를 추가하려 할 때
> 기존 `uk_game_platform_id`가 남아있으면 중복 constraint로 인해 기동이 실패하거나
> 저장 시 의도치 않은 제약이 걸릴 수 있다.

---

## 데이터 복구

### Step 1 — 영향받은 유저 확인

```sql
SELECT cu.id, cu.username
FROM chesscom_user cu
WHERE NOT EXISTS (
    SELECT 1 FROM user_daily_game_stat s
    WHERE s.user_id = cu.id AND s.platform = 'CHESSCOM'
);
```

### Step 2 — 영향받은 유저 데이터 정리

`(:affected_user_ids)` 자리에 Step 1 결과 id 목록을 넣는다.

```sql
-- 잘못 스킵된 게임 데이터 삭제 (코드 픽스 후 재수집으로 복구)
DELETE FROM game                 WHERE user_id IN (:affected_user_ids) AND platform = 'CHESSCOM';
DELETE FROM user_daily_game_stat WHERE user_id IN (:affected_user_ids) AND platform = 'CHESSCOM';
DELETE FROM user_color_stat      WHERE user_id IN (:affected_user_ids) AND platform = 'CHESSCOM';
DELETE FROM user_first_move_stat WHERE user_id IN (:affected_user_ids) AND platform = 'CHESSCOM';
DELETE FROM user_perf_stat       WHERE user_id IN (:affected_user_ids) AND platform = 'CHESSCOM';

-- COMPLETED → FAILED로 변경해 스케줄러가 cursor=null(전체 재수집)로 실행하게 유도
UPDATE sync_job
SET status = 'FAILED'
WHERE user_id IN (:affected_user_ids)
  AND platform = 'CHESSCOM'
  AND status = 'COMPLETED';
```

> `sync_job`을 `FAILED`로 바꾸는 이유:
> `ScheduledSyncTrigger.scheduleChesscomUsers()`는 마지막 job 상태가 `COMPLETED`이면
> cursor=전월로 **증분 수집**만 실행한다. `COMPLETED`가 아닌 상태여야
> cursor=null → **전체 아카이브 수집**이 트리거된다.

### Step 3 — 자동 복구 확인

데이터 정리 후 다음 30분 스케줄러(`ScheduledSyncTrigger`) 트리거 시
해당 유저들은 전체 수집 → 게임 저장 → 집계까지 자동으로 완료된다.

---

## 배포 순서 요약

```
1. DB: ALTER TABLE (constraint 교체)
2. 코드 배포
3. DB: Step 1 쿼리로 영향 유저 확인
4. DB: Step 2 쿼리로 데이터 정리
5. 다음 스케줄러 트리거 후 집계 데이터 채워졌는지 확인
```
