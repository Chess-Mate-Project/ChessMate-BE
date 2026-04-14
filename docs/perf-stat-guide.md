# user_perf_stat 리팩토링 가이드

## 배경

기존에는 Lichess, Chess.com 각각 별도의 요약 통계 테이블(`lichess_user_stat`, `chesscom_user_stat`)을 운영했다.
그러나 서비스의 핵심 기능인 **레이팅 → 티어 변환**을 위해서는 타임클래스(bullet / blitz / rapid / classical)별 레이팅이 필수적이다.
기존 테이블은 타임클래스 구분 없이 전체 집계만 저장하므로 이 요구사항을 충족할 수 없었다.

## 변경 내용

### Before
| 테이블 | 플랫폼 | 내용 |
|--------|--------|------|
| `lichess_user_stat` | Lichess | 전체 통합 집계 (all_games, wins, losses, draws + title, 가입일 등) |
| `chesscom_user_stat` | Chess.com | 전체 통합 집계 |

### After
| 테이블 | 플랫폼 | 내용 |
|--------|--------|------|
| `user_perf_stat` | 공통 | 타임클래스별 레이팅 + 승/무/패 (platform, time_class, rating, games, wins, losses, draws) |

## 아키텍처

```
게임 수집 완료
    ├─ GameStatAggregator.aggregate()  → user_daily_game_stat, user_color_stat, user_first_move_stat
    └─ PerfStatFetcher.fetch()         → user_perf_stat  (플랫폼 API 직접 호출)
```

### 왜 PerfStatFetcher를 분리했는가?
- 레이팅 데이터는 게임 테이블에서 집계 불가 (게임 레코드에는 ratingDiff만 존재)
- 플랫폼 API 직접 호출 필요
  - Lichess: `GET /api/user/{username}` → `perfs.{timeClass}.rating + games`
  - Chess.com: `GET /pub/player/{username}/stats` → `chess_{timeClass}.last.rating + record`

### 승/무/패 출처
| 플랫폼 | 레이팅 | 승/무/패 |
|--------|--------|----------|
| Lichess | `/api/user/{username}` perfs | `user_color_stat` (WHITE + BLACK 합산) |
| Chess.com | `/pub/player/{username}/stats` record | 동일 API의 record 필드 |

> Lichess의 `/api/user/{username}` perfs는 레이팅과 게임 수만 제공하고 승/무/패를 직접 제공하지 않는다.
> 따라서 이미 집계된 `user_color_stat`에서 해당 타임클래스의 WHITE + BLACK 합산값을 사용한다.

## 삭제된 파일

### Domain
- `lichess/userStat/LichessUserStat.java`
- `lichess/userStat/LichessUserStatRepository.java`
- `chesscom/userStat/ChesscomUserStat.java`
- `chesscom/userStat/ChesscomUserStatRepository.java`

### Infra-persistence
- `lichess/user/entity/LichessUserStatEntity.java`
- `lichess/userStat/**` (JpaRepository, Mapper, RepositoryImpl)
- `chesscom/userStat/**` (Entity, JpaRepository, Mapper, RepositoryImpl)

### API
- `stat/dto/LichessUserStatResponse.java`
- `stat/dto/ChesscomUserStatResponse.java`

### API 엔드포인트
- `GET /api/stat/lichess-summary` → 삭제
- `GET /api/stat/chesscom-summary` → 삭제

## 추가된 파일

| 경로 | 설명 |
|------|------|
| `chessmate-domain/.../stat/UserPerfStat.java` | 도메인 모델 |
| `chessmate-domain/.../stat/UserPerfStatRepository.java` | 리포지토리 인터페이스 |
| `chessmate-infra-persistence/.../stat/entity/UserPerfStatJpaEntity.java` | JPA 엔티티 (table: `user_perf_stat`) |
| `chessmate-infra-persistence/.../stat/jpaRepository/UserPerfStatJpaRepository.java` | JPA 리포지토리 |
| `chessmate-infra-persistence/.../stat/mapper/UserPerfStatMapper.java` | 매퍼 |
| `chessmate-infra-persistence/.../stat/repositoryImpl/UserPerfStatRepositoryImpl.java` | 구현체 |
| `chessmate-worker/.../PerfStatFetcher.java` | 플랫폼 API 호출 컴포넌트 |
| `chessmate-api/.../stat/dto/UserPerfStatResponse.java` | 응답 DTO |
| `chessmate-external/.../dto/chesscom/ChesscomPlayerStatsResponse.java` 外 3개 | Chess.com stats API DTO |

## API 명세 변경

| Before | After |
|--------|-------|
| `GET /api/stat/lichess-summary[?timeClass=]` | 삭제 |
| `GET /api/stat/chesscom-summary[?timeClass=]` | 삭제 |
| (없음) | `GET /api/stat/perf?platform=&[timeClass=]` |

## DB 스키마

```sql
CREATE TABLE user_perf_stat (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    platform   VARCHAR(20)  NOT NULL,
    time_class VARCHAR(20)  NOT NULL,
    rating     INT          NOT NULL,
    games      INT          NOT NULL,
    wins       INT          NOT NULL,
    losses     INT          NOT NULL,
    draws      INT          NOT NULL,
    INDEX idx_perf_stat_user_platform (user_id, platform),
    UNIQUE KEY uk_perf_stat (user_id, platform, time_class)
);
```
