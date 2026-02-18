# UserPerf 중복 생성 감지 로깅 강화 보고서

## 📋 요약
`LichessApiTaskHandler.java`의 `syncUserPerf()` 메서드에 상세한 로깅을 추가하여, 중복 데이터 생성 문제를 실시간으로 감지할 수 있도록 개선했습니다.

---

## 🔍 추가된 로깅 항목

### 1️⃣ DB 조회 단계
```
[Worker-PERF] [RAPID] === DB 조회 시작 === userId=1, gameType=RAPID
```
**목적**: 언제 DB 조회가 시작되었는지 추적

#### ✅ DB 조회 결과 - 데이터 발견 (기존 데이터 있음)
```
[Worker-PERF] [RAPID] [DB-FOUND] 기존 데이터 발견 | id=100, rating=1600, games=50, rated=48, QueryTime=2ms
```
- **언제 사용**: 이미 user_id + game_type 조합이 DB에 존재할 때
- **정보**: id, rating, 게임 수, 레이티드 게임 수, 쿼리 소요 시간
- **의미**: UPDATE 경로로 진행될 예정

#### ⚠️ DB 조회 결과 - 데이터 미발견 (새로 생성 예정)
```
[Worker-PERF] [RAPID] [DB-NOT-FOUND] 기존 데이터 없음 | userId=1, gameType=RAPID | 신규 INSERT 예정 | QueryTime=1ms
```
- **언제 사용**: 해당 user_id + game_type 조합이 없을 때
- **의미**: INSERT 경로로 진행될 예정
- **⚠️ 중요**: 같은 조합으로 **동시에 여러 번** 이 로그가 나타나면 **Race Condition 의심**

---

### 2️⃣ UPDATE 경로 (기존 데이터 업데이트)

#### 단계 1: UPDATE 시작
```
[Worker-PERF] [RAPID] [UPDATE] 시작 | id=100
```

#### 단계 2: 변경 사항 감지
```
[Worker-PERF] [RAPID] [UPDATE-CHANGE-DETECTED] id=100 | 변경 사항 감지
[Worker-PERF] [RAPID]   [FIELD-CHANGE] rating: 1500 → 1600
[Worker-PERF] [RAPID]   [FIELD-CHANGE] games: 45 → 50
[Worker-PERF] [RAPID]   [FIELD-CHANGE] rated: 43 → 48
```
- 어떤 필드가 변경되었는지 필드별로 표시
- 이전 값 → 새 값 형식

#### 단계 3: UPDATE 완료 (성공)
```
[Worker-PERF] [RAPID] [UPDATE-SUCCESS] ✓ | id=100, rating=1600, games=50, SaveTime=3ms
```
- 저장 소요 시간 포함

#### 또는 변경 사항 없음 (스킵)
```
[Worker-PERF] [RAPID] [UPDATE-SKIP] id=100 | 변경 사항 없음 - 스킵
```
- 기존 데이터와 새 데이터가 동일하면 UPDATE하지 않음

---

### 3️⃣ INSERT 경로 (새로운 데이터 생성)

#### 단계 1: INSERT 시작
```
[Worker-PERF] [RAPID] [INSERT] 시작 | userId=1
[Worker-PERF] [RAPID] [INSERT-DATA] rating=1500, games=50, rated=48, wins=25, losses=23, draws=2
```

#### 단계 2: INSERT 완료 (성공)
```
[Worker-PERF] [RAPID] [INSERT-SUCCESS] ✓ | newId=101, userId=1, gameType=RAPID, rating=1500, SaveTime=2ms
```
- 새로 생성된 ID
- 모든 중요 정보
- 저장 소요 시간

---

### 4️⃣ 에러 발생 시
```
[Worker-PERF] [RAPID] [ERROR] ✗ | userId=1, gameType=RAPID, message=Connection timeout, exceptionType=SQLException
```
- 어떤 GameType에서 에러가 났는지
- 에러 메시지
- 예외 클래스명

---

## 🎯 중복 생성 감지 방법

### 시나리오: Race Condition으로 인한 중복 생성

**정상 케이스 (문제 없음):**
```
2025-02-15 14:00:00 [Worker-PERF] [RAPID] === DB 조회 시작 === userId=1, gameType=RAPID
2025-02-15 14:00:00 [Worker-PERF] [RAPID] [DB-FOUND] 기존 데이터 발견 | id=100, rating=1500...
2025-02-15 14:00:00 [Worker-PERF] [RAPID] [UPDATE-CHANGE-DETECTED] id=100 | 변경 사항 감지
2025-02-15 14:00:00 [Worker-PERF] [RAPID] [UPDATE-SUCCESS] ✓ | id=100, rating=1600...
```

**비정상 케이스 (중복 생성 의심):**
```
2025-02-15 14:00:00 [Worker-PERF] [RAPID] === DB 조회 시작 === userId=1, gameType=RAPID
2025-02-15 14:00:00 [Worker-PERF] [RAPID] [DB-NOT-FOUND] 기존 데이터 없음 | userId=1, gameType=RAPID | INSERT 예정
2025-02-15 14:00:00 [Worker-PERF] [RAPID] [INSERT-SUCCESS] ✓ | newId=101, userId=1...

2025-02-15 14:00:00 [Worker-PERF] [RAPID] === DB 조회 시작 === userId=1, gameType=RAPID  ← 같은 시간!
2025-02-15 14:00:00 [Worker-PERF] [RAPID] [DB-NOT-FOUND] 기존 데이터 없음 | userId=1, gameType=RAPID  ← ⚠️ NULL 또 반환!
2025-02-15 14:00:00 [Worker-PERF] [RAPID] [INSERT-SUCCESS] ✓ | newId=102, userId=1...
```

**확인 포인트:**
- 같은 user + gameType 조합으로 **동시에** `[INSERT-SUCCESS]`가 여러 번 나타남
- 생성되는 ID가 연속적으로 증가 (101, 102, 103...)
- **같은 timestamp**에 여러 INSERT가 발생

---

## 📊 로그 레벨별 정보

| 레벨 | 로그 태그 | 설명 |
|------|----------|------|
| **DEBUG** | `[API 호출 시작]` | API 요청 전 |
| **DEBUG** | `[API 응답 수신]` | API 응답 받은 데이터 |
| **DEBUG** | `[DB 조회:]` | DB 조회 전 |
| **INFO** | `[=== DB 조회 시작 ===]` | 실제 DB 조회 시작 |
| **INFO** | `[DB-FOUND]` | 기존 데이터 발견 |
| **WARN** | `[DB-NOT-FOUND]` | 기존 데이터 없음 (새 생성 예정) |
| **INFO** | `[UPDATE]` | UPDATE 시작 |
| **INFO** | `[UPDATE-CHANGE-DETECTED]` | 변경 사항 감지 |
| **INFO** | `[FIELD-CHANGE]` | 필드별 변경사항 |
| **INFO** | `[UPDATE-SUCCESS]` | UPDATE 완료 |
| **INFO** | `[INSERT]` | INSERT 시작 |
| **INFO** | `[INSERT-SUCCESS]` | INSERT 완료 |
| **ERROR** | `[ERROR]` | 예외 발생 |

---

## 🔎 로그 분석 팁

### 1. tail로 실시간 모니터링
```bash
docker logs -f chessladder-api | grep "\[Worker-PERF\]"
```

### 2. 특정 유저의 로그만 보기
```bash
docker logs chessladder-api | grep "userId=1"
```

### 3. 중복 생성 의심 케이스 찾기
```bash
docker logs chessladder-api | grep "\[INSERT-SUCCESS\]" | grep "userId=1"
# 같은 userId가 여러 번 나타나면 중복 생성 의심
```

### 4. 동시 요청 감지
```bash
docker logs chessladder-api | grep "\[DB-NOT-FOUND\]"
# 같은 timestamp에 여러 개 발생 → Race Condition
```

---

## 📈 예상되는 로그 흐름 (정상)

**User 1이 RAPID 게임을 한 경우:**

```
14:00:00 [Worker-PERF] [RAPID] === DB 조회 시작 === userId=1, gameType=RAPID
14:00:00 [Worker-PERF] [RAPID] [DB-FOUND] 기존 데이터 발견 | id=100, rating=1500, games=45, rated=43, QueryTime=2ms
14:00:00 [Worker-PERF] [RAPID] [UPDATE] 시작 | id=100
14:00:00 [Worker-PERF] [RAPID] [UPDATE-CHANGE-DETECTED] id=100 | 변경 사항 감지
14:00:00 [Worker-PERF] [RAPID]   [FIELD-CHANGE] rating: 1500 → 1520
14:00:00 [Worker-PERF] [RAPID]   [FIELD-CHANGE] games: 45 → 46
14:00:00 [Worker-PERF] [RAPID] [UPDATE-SUCCESS] ✓ | id=100, rating=1520, games=46, SaveTime=3ms
```

**User 2가 처음 BLITZ를 플레이한 경우:**

```
14:00:05 [Worker-PERF] [BLITZ] === DB 조회 시작 === userId=2, gameType=BLITZ
14:00:05 [Worker-PERF] [BLITZ] [DB-NOT-FOUND] 기존 데이터 없음 | userId=2, gameType=BLITZ | 신규 INSERT 예정 | QueryTime=1ms
14:00:05 [Worker-PERF] [BLITZ] [INSERT] 시작 | userId=2
14:00:05 [Worker-PERF] [BLITZ] [INSERT-SUCCESS] ✓ | newId=500, userId=2, gameType=BLITZ, rating=1400, SaveTime=2ms
```

---

## ✅ 다음 단계

1. **로그 수집**: Worker를 배포 후 충분한 시간 로그 수집
2. **분석**: 위의 "중복 생성 감지 방법"을 따라 로그 분석
3. **결과 보고**: 
   - Race Condition이 감지되면 → Pessimistic Lock 적용 필요
   - Unique 제약 조건 위반 에러가 나면 → DB 레벨 중복 방지 성공
   - 정상 흐름만 보이면 → 문제 해결 완료

---

## 📝 수정 내용 요약

| 항목 | 변경 전 | 변경 후 |
|------|--------|--------|
| DB 조회 | 2줄 | 3줄 (타이밍 포함) |
| UPDATE 경로 | 6줄 | 12줄 (필드별 변경사항) |
| INSERT 경로 | 4줄 | 8줄 |
| 에러 로깅 | 1줄 | 2줄 (예외 클래스명 포함) |
| 소요 시간 추적 | 없음 | 추가됨 (QueryTime, SaveTime) |

**파일**: `LichessApiTaskHandler.java`
**메서드**: `syncUserPerf()`
**총 추가 로그**: 약 15줄

