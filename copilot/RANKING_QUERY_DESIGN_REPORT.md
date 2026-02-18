# 랭킹 기능 쿼리 설계 보고서

## 1. 요구사항 분석

### 기능 요구사항
- GameType 기반 사용자 전체 검색
- Rating 기준 랭킹 정렬
- Rated Game < 50인 사용자 제외
- 사용자 본인의 랭킹 및 레이팅 포함

### 응답 구조
```json
{
    "my_rank": "n",
    "my_rating": "n",
    "ranking": [
        {
            "username": "string",
            "rating": "number",
            "rank": "number",
            "banner": "string",
            "profile": "string",
            "userId": "number"
        }
    ]
}
```

---

## 2. 쿼리 설계

### 2.1 메인 랭킹 조회 쿼리

#### 쿼리명: `findRankingByGameType`

```sql
SELECT up 
FROM UserPerfEntity up 
WHERE up.gameType = :gameType 
  AND up.rated >= 50 
ORDER BY up.rating DESC
```

**목적**: 특정 게임타입의 모든 유효한 사용자를 레이팅 순서대로 조회

**조건**:
- `gameType = :gameType`: 특정 게임타입 필터링 (BULLET, BLITZ, RAPID, CLASSICAL)
- `rated >= 50`: 50게임 이상 플레이한 사용자만 포함 (신뢰도 있는 데이터)
- `ORDER BY rating DESC`: 높은 레이팅순 정렬 (상위 랭크가 앞)

**반환값**: List<UserPerfEntity> (순서대로 rank가 결정됨)

**성능 최적화 포인트**:
- `gameType` 및 `rated` 컬럼에 인덱스 필수
- `rating DESC` 정렬을 위해 `(gameType, rated, rating DESC)` 복합 인덱스 권장

---

### 2.2 사용자 순위 계산 쿼리

#### 쿼리명: `countUsersBetterRating`

```sql
SELECT COUNT(up) 
FROM UserPerfEntity up 
WHERE up.gameType = :gameType 
  AND up.rated >= 50 
  AND up.rating > :rating
```

**목적**: 특정 사용자보다 높은 레이팅을 가진 사용자 수 계산 → 순위 결정

**로직**: 
```
my_rank = countUsersBetterRating(...) + 1
```

**조건**:
- `gameType = :gameType`: 같은 게임타입 내에서만 비교
- `rated >= 50`: 유효한 사용자만 포함
- `rating > :rating`: 사용자 레이팅보다 높은 사람 수 계산

**반환값**: int (더 높은 순위 사용자 수)

**성능 최적화**:
- `(gameType, rated, rating)` 복합 인덱스로 COUNT 쿼리 최적화

---

## 3. 데이터베이스 인덱스 설계

### 권장 인덱스 구조

```sql
-- 복합 인덱스 (가장 효율적)
CREATE INDEX idx_userperf_ranking 
ON user_perfs (game_type, rated DESC, rating DESC);

-- 또는 개별 인덱스
CREATE INDEX idx_userperf_gametype ON user_perfs (game_type);
CREATE INDEX idx_userperf_rated ON user_perfs (rated);
CREATE INDEX idx_userperf_rating ON user_perfs (rating);
```

---

## 4. 쿼리 실행 흐름

### 4.1 전체 랭킹 조회 흐름

```
1. findRankingByGameType(GameType.RAPID) 호출
   └─ WHERE gameType = 'RAPID' AND rated >= 50
   └─ ORDER BY rating DESC
   └─ 모든 유효 사용자 조회 (예: 500명)

2. 반환된 List를 순회하며 rank 정보 생성
   └─ 1번 사용자: rank = 1
   └─ 2번 사용자: rank = 2
   └─ ...
   └─ 500번 사용자: rank = 500

3. User 엔티티 JOIN으로 username, banner, profile 추가
   └─ UserPerfEntity.userId → User.id JOIN

4. RankingResponse 구성
```

### 4.2 사용자 본인 순위 조회 흐름

```
1. 사용자의 UserPerf 조회
   └─ findByUserIdAndGameType(userId, GameType.RAPID)
   └─ my_rating = userPerf.rating 추출

2. 순위 계산
   └─ countUsersBetterRating(GameType.RAPID, my_rating) 호출
   └─ COUNT = X (나보다 높은 사람 X명)
   └─ my_rank = X + 1

3. Response 생성
   └─ my_rank, my_rating, ranking list 포함
```

---

## 5. SQL 실행 계획

### 5.1 findRankingByGameType 실행 계획

```
Index Range Scan on idx_userperf_ranking
├── Filter: game_type = 'RAPID'
├── Filter: rated >= 50
└── Sort: rating DESC (인덱스로 커버되어 추가 정렬 불필요)
Result: O(log n) 검색 + O(n) 스캔
```

### 5.2 countUsersBetterRating 실행 계획

```
Index Range Scan on idx_userperf_ranking
├── Filter: game_type = 'RAPID' 
├── Filter: rated >= 50
├── Filter: rating > 1800 (매개변수)
└── Aggregate: COUNT(*)
Result: O(log n) 검색 + O(m) 스캔 (m = 1800 이상인 사용자 수)
```

---

## 6. 성능 고려사항

| 구분 | 설명 | 최적화 방법 |
|------|------|-----------|
| **쿼리 1** | 전체 랭킹 조회 (대량 데이터) | 복합 인덱스, 페이지네이션 |
| **쿼리 2** | 순위 계산 (단일 사용자) | 범위 인덱스 활용 |
| **메모리** | 전체 사용자 메모리 로드 | 페이지네이션 (100명씩) 권장 |
| **응답시간** | 일반적으로 < 100ms | 캐싱 (1시간 TTL) 추천 |

---

## 7. 캐싱 전략

```
Cache Key: stat:ranking:{gameType}
TTL: 3600 (1시간)
갱신: 새 게임 완료 시 or 수동 갱신

my_rank는 캐싱 불가 (개인별 데이터)
├─ countUsersBetterRating 동적 계산 필수
└─ 부하 최소화: 사용자 접근 시에만 계산
```

---

## 8. 요약

### 쿼리 2개 조합으로 요구사항 충족

| 쿼리명 | 역할 | 입력 | 출력 |
|--------|------|------|------|
| `findRankingByGameType` | 랭킹 목록 조회 | GameType | List<UserPerf> |
| `countUsersBetterRating` | 순위 계산 | GameType, Rating | int (나보다 높은 사람) |

### 응답 구성 프로세스

```
1단계: findRankingByGameType(RAPID) 호출
       → 500명 사용자 리스트 반환 (정렬됨)

2단계: 현재 사용자의 UserPerf 조회
       → my_rating = 1800

3단계: countUsersBetterRating(RAPID, 1800) 호출
       → 150명 반환
       → my_rank = 151

4단계: User 정보 JOIN으로 username, banner, profile 추가

5단계: RankingResponse 구성
       {
         "my_rank": 151,
         "my_rating": 1800,
         "ranking": [ ... 500명 리스트 ... ]
       }
```

---

## 결론

**설계 특징**:
✅ Rated Game >= 50 조건으로 신뢰도 높은 순위  
✅ GameType별 독립적 관리로 확장성 우수  
✅ 복합 인덱스로 O(log n + k) 성능 보장  
✅ COUNT 쿼리로 효율적 순위 계산  
✅ 사용자별 동적 순위 반영 가능  

**예상 응답시간**:
- 전체 랭킹 조회: ~50ms
- 순위 계산: ~10ms
- 총 응답시간: < 100ms

