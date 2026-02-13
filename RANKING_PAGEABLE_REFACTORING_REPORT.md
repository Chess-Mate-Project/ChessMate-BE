# Ranking API Pageable 리팩토링 보고서

## 📋 요약

랭킹 API의 페이지네이션 처리를 수동 계산 방식에서 Spring의 **Pageable** 인터페이스를 사용하는 방식으로 리팩토링하였습니다.

---

## 🎯 리팩토링 목적

1. **표준화**: Spring Data의 표준 페이징 방식 적용
2. **유연성 향상**: 클라이언트에서 페이지 크기(size) 조정 가능
3. **코드 간결화**: 하드코딩된 `PAGE_SIZE` 상수 제거
4. **확장성**: 정렬(Sort) 기능 추가 시 용이

---

## 🔧 주요 변경 사항

### 1. RankController 변경

#### Before
```java
@GetMapping("/ranking")
public ResponseEntity<SuccessResponse<RankingResponse>> getRanking(
    @AuthenticationPrincipal UserPrincipal userPrincipal,
    @RequestParam(defaultValue = "RAPID") GameType gameType,
    @RequestParam(defaultValue = "0") int page  // ❌ int 타입 페이지 번호만 받음
)
```

#### After
```java
@GetMapping("/ranking")
public ResponseEntity<SuccessResponse<RankingResponse>> getRanking(
    @AuthenticationPrincipal UserPrincipal userPrincipal,
    @RequestParam(defaultValue = "RAPID") GameType gameType,
    @PageableDefault(size = 20) Pageable pageable  // ✅ Pageable 객체로 변경
)
```

**변경 포인트:**
- `int page` → `Pageable pageable`
- `@PageableDefault(size = 20)` 어노테이션으로 기본값 설정
- 클라이언트에서 `?page=0&size=20` 형태로 요청 가능

---

### 2. RankService 변경

#### Before
```java
private static final int PAGE_SIZE = 20;  // ❌ 하드코딩된 상수

public RankingResponse getRankers(User user, GameType gameType, int page) {
    // 수동 페이지네이션 계산
    int totalCount = allRankings.size();
    long totalPages = (long) Math.ceil((double) totalCount / PAGE_SIZE);
    int startIndex = page * PAGE_SIZE;
    int endIndex = Math.min(startIndex + PAGE_SIZE, totalCount);
}
```

#### After
```java
// ✅ PAGE_SIZE 상수 제거

public RankingResponse getRankers(User user, GameType gameType, Pageable pageable) {
    // Pageable을 활용한 페이지네이션 계산
    int totalCount = allRankings.size();
    int pageSize = pageable.getPageSize();           // ✅ 동적으로 size 가져옴
    int currentPage = pageable.getPageNumber();      // ✅ 페이지 번호
    long totalPages = (long) Math.ceil((double) totalCount / pageSize);
    int startIndex = (int) pageable.getOffset();     // ✅ offset 계산 자동화
    int endIndex = Math.min(startIndex + pageSize, totalCount);
}
```

**변경 포인트:**
- `private static final int PAGE_SIZE = 20` 상수 제거
- `page * PAGE_SIZE` → `pageable.getOffset()` (자동 계산)
- `PAGE_SIZE` → `pageable.getPageSize()` (동적 값)
- `page` → `pageable.getPageNumber()`

---

### 3. 로그 메시지 개선

#### Before
```java
log.info("[Ranking Request] gameType={}, page={}, pageSize={}, user={}",
    gameType, page, PAGE_SIZE, user != null ? user.getUsername() : "Anonymous");
```

#### After
```java
log.info("[Ranking Request] gameType={}, page={}, pageSize={}, user={}",
    gameType, pageable.getPageNumber(), pageable.getPageSize(),
    user != null ? user.getUsername() : "Anonymous");
```

---

## 📊 비교표

| 항목 | Before | After |
|------|--------|-------|
| **파라미터 타입** | `int page` | `Pageable pageable` |
| **페이지 크기** | 고정 (20) | 동적 (기본값 20) |
| **API 요청 예시** | `?page=0` | `?page=0&size=20` |
| **offset 계산** | `page * PAGE_SIZE` | `pageable.getOffset()` |
| **확장성** | 정렬 기능 추가 어려움 | `Sort` 파라미터 추가 용이 |
| **Spring 표준** | ❌ 커스텀 방식 | ✅ Spring Data 표준 |

---

## 🚀 새로운 API 사용법

### 기본 요청 (페이지 0, 크기 20)
```http
GET /api/rank/ranking?gameType=RAPID
```

### 페이지 지정
```http
GET /api/rank/ranking?gameType=RAPID&page=2
```

### 페이지 크기 변경 (예: 50개씩)
```http
GET /api/rank/ranking?gameType=RAPID&page=0&size=50
```

### 페이지 + 크기 조합
```http
GET /api/rank/ranking?gameType=BLITZ&page=3&size=10
```

---

## ✅ 검증 결과

- **컴파일 에러**: 없음
- **IntelliJ 검사**: 경고 없음
- **하위 호환성**: 기존 `?page=0` 요청도 동작 (size는 기본값 20 적용)

---

## 📌 향후 확장 가능성

### 1. 정렬 기능 추가
```java
@PageableDefault(size = 20, sort = "rating", direction = Sort.Direction.DESC)
Pageable pageable
```

### 2. DB 레벨 페이징 (성능 최적화)
현재는 **캐시에서 전체 조회 → 메모리 페이징** 방식입니다.
추후 DB 쿼리에 `Pageable`을 직접 전달하여 최적화 가능:

```java
// UserPerfRepository에 Pageable 추가
Page<UserPerf> findRankingByGameType(GameType gameType, Pageable pageable);
```

---

## 📁 수정 파일 목록

1. `chessmate-api/src/main/java/com/chessmate/api/rank/controller/RankController.java`
2. `chessmate-api/src/main/java/com/chessmate/api/rank/service/RankService.java`

---

## 🔍 주요 개선 효과

✅ **코드 가독성 향상**: Spring 표준 방식 사용
✅ **유연성 증가**: 페이지 크기를 클라이언트에서 조정 가능
✅ **유지보수성**: 하드코딩 제거로 설정 변경 용이
✅ **확장성**: 정렬 기능 추가 시 최소한의 코드 수정

---

**작성일**: 2026-02-13
**작성자**: Claude (AI Assistant)
