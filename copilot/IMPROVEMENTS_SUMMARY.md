# ChessMate 백엔드 개선사항 요약

## 1. 쿠키 도메인 에러 수정

### 문제
- **에러**: `java.lang.IllegalArgumentException: =: invalid cookie domain char '61'`
- **원인**: 쿠키 도메인이 잘못된 형식으로 전달됨 (ASCII 코드 61 = `=` 문자)

### 해결
- 파일: `chessmate-api/src/main/resources/application-prod.yml`
- 변경: `cookie.domain: ${COOKIE_DOMAIN:.chessladder.org}` → `cookie.domain: ${COOKIE_DOMAIN:chessladder.org}`
- 설명: RFC 6265 호환성을 위해 도메인 앞의 점(.) 제거
  - `.chessladder.org`: 오류 발생 (특수문자 포함)
  - `chessladder.org`: 올바른 형식 (브라우저가 자동으로 서브도메인 공유 활성화)

---

## 2. 이미지 업데이트 데이터 저장 문제 개선

### 문제
- 프로필/배너 이미지 업데이트 후 데이터가 저장되지 않는 것처럼 보임
- 로그: `savedDescription=null` (실제로는 저장되었으나 로깅 부분에서 표시 안 됨)

### 해결

#### 2-1. UserRepositoryImpl 수정
- 파일: `chessmate-infra-persistence/src/main/java/.../UserRepositoryImpl.java`
- 변경: `updateProfileImage()` 메서드에서 `save()` → `saveAndFlush()` 변경
- 이유: `updateBannerImage()`와의 일관성 유지, 즉시 DB 반영 보장

#### 2-2. UserService 로깅 강화
- 파일: `chessmate-api/src/main/java/.../UserService.java`
- 개선사항:
  ```java
  // 저장 후 다시 조회하여 실제 저장 여부 확인
  User verifyUser = userRepository.findById(u.getId()).orElseThrow();
  log.debug("[DB 저장 검증] userId={}, verifyDescription={}", 
    verifyUser.getId(), verifyUser.getDescription());
  ```
- 이유: 설명 업데이트 시 실제 DB 저장 여부를 즉시 확인

#### 2-3. 이미지 업로드 흐름 (정상 작동)
1. `ImageService.generateUploadUrl()`: Presigned URL 생성
2. 프론트: R2에 이미지 업로드
3. `ImageService.completeUpload()`: 이미지 키를 DB에 저장 + 캐시 삭제
   - `@Transactional` 적용으로 트랜잭션 보장
   - `saveAndFlush()` 사용으로 즉시 DB 반영
4. `UserService.getUserProfile()`: 프로필 조회
   - Cache-Miss → DB에서 새 이미지 URL 조회 → 캐시 저장

---

## 3. WebClient User-Agent 설정 (이미 적용됨)

### 상태
✅ **이미 올바르게 구현됨**

- 파일: `chessmate-external/src/main/java/.../WebclientConfig.java`
- 모든 Lichess/Chess.com API 요청에 User-Agent 자동 추가
  ```java
  .defaultHeader("user-agent", "ChessLadder/1.0 (https://chessladder.org)")
  ```
- 필터링 로깅으로 모든 요청 추적 가능

### Lichess IP 차단 우회 전략
1. **User-Agent 설정** ✅ 완료
2. **Redis Queue 순차 처리** ✅ 구현됨 (Worker에서 배치 처리)
3. **요청 간격 제어** - 현재 배치 단위로 처리되므로 동시 요청 없음
4. **IP 변경** - 필요시 프록시/로드밸런싱 검토

---

## 4. Chess.com 도메인 모델 추가

### 새로 생성된 파일

#### 4-1. ChessComUserStats 도메인 모델
- 파일: `chessmate-domain/.../ChessComUserStats.java`
- 역할: Chess.com의 게임 타입별 통계 관리
- 주요 필드:
  - `rating`: 레이팅
  - `rd`: Rating Deviation (정수)
  - `ratedGamesCount`: 레이팅 게임 판수
  - `win`, `loss`, `draw`: 전적
  - `bestRating`, `bestRatingDate`: 최고 레이팅
- 사용되는 게임 타입: `daily`, `rapid`, `blitz`, `bullet`

#### 4-2. ChessComProfile 도메인 모델
- 파일: `chessmate-domain/.../ChessComProfile.java`
- 역할: Chess.com 사용자 프로필 정보 관리
- 주요 필드:
  - `playerId`: Chess.com 고유 ID
  - `username`: 사용자명
  - `status`: 계정 상태 (basic, premium, mod, staff 등)
  - `avatar`, `location`: 프로필 이미지, 위치
  - `followers`: 팔로워 수
  - `isStreamer`, `twitchUrl`: 스트리밍 정보
  - `fideRating`: FIDE 레이팅
- 데이터 동기화: `syncedAt` 필드로 추적

#### 4-3. ChessComProfileRepository 인터페이스
- 파일: `chessmate-domain/.../ChessComProfileRepository.java`
- 주요 메서드:
  - `findByUserId()`, `findByPlayerId()`, `findByUsername()`
  - `save()`, `update()`, `delete()`

### DB 스키마 설계 참고

```sql
-- Chess.com 통계 테이블
CREATE TABLE chesscom_user_stats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  game_type VARCHAR(50) NOT NULL,  -- daily, rapid, blitz, bullet
  rating INT,
  rd INT,  -- Rating Deviation (정수)
  rated_games_count INT,
  win INT,
  loss INT,
  draw INT,
  best_rating INT,
  best_rating_date TIMESTAMP,
  best_game_url VARCHAR(255),
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Chess.com 프로필 테이블
CREATE TABLE chesscom_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  player_id BIGINT NOT NULL UNIQUE,
  profile_url VARCHAR(255),
  url VARCHAR(255),
  username VARCHAR(100),
  title VARCHAR(20),
  status VARCHAR(50),
  name VARCHAR(100),
  avatar VARCHAR(255),
  location VARCHAR(100),
  country VARCHAR(255),
  joined BIGINT,  -- Unix timestamp
  last_online BIGINT,
  followers BIGINT,
  is_streamer BOOLEAN,
  twitch_url VARCHAR(255),
  fide_rating INT,
  synced_at TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 5. DRAW 결과 처리 (정상 작동)

### 상태
✅ **DRAW 데이터 정상 저장됨**

### 처리 흐름
1. **LichessGameProcessor.resolveMyGameResult()**
   ```java
   if(game.status().equals("draw")) {
     return GameResult.DRAW;  // ✅ 정상 처리
   }
   ```

2. **ColorStat 저장**
   - DRAW 게임도 UserColorStat 생성됨
   - `result = GameResult.DRAW`로 저장

3. **FirstMoveStat 저장**
   - DRAW 게임의 첫 수도 기록됨
   - ColorStat과 함께 저장

4. **DailyStreak 저장**
   ```java
   .draw(result == GameResult.DRAW ? 1 : 0)  // ✅ DRAW 카운트
   ```

---

## 6. 권장 사항

### 단기 (즉시)
1. ✅ 쿠키 도메인 설정 수정 (이미 완료)
2. ✅ UserRepository `saveAndFlush()` 적용 (이미 완료)
3. ✅ UserService 로깅 강화 (이미 완료)

### 중기 (1-2주)
1. ChessComUserStats/ChessComProfile 엔티티/매퍼/리포지토리 구현
2. Chess.com API 통계 수집 배치 작성
3. 프론트 인증 구조 상세 명세 작성

### 장기 (1개월+)
1. IP 차단 우회: 프록시 서버 또는 스마트 요청 스케줄링
2. 랭킹 페이지네이션 최적화
3. 캐시 전략 모니터링 및 TTL 조정

---

## 파일 변경 사항 요약

| 파일 | 변경사항 | 이유 |
|------|--------|------|
| application-prod.yml | 쿠키 도메인: `.chessladder.org` → `chessladder.org` | RFC 6265 호환성 |
| UserRepositoryImpl.java | `updateProfileImage()`: `save()` → `saveAndFlush()` | 즉시 DB 반영 보장 |
| UserService.java | 설명 업데이트 로깅 강화 | 실제 저장 여부 검증 |
| ChessComUserStats.java | **신규** | Chess.com 통계 관리 |
| ChessComProfile.java | **신규** | Chess.com 프로필 관리 |
| ChessComProfileRepository.java | **신규** | Chess.com 프로필 CRUD |


