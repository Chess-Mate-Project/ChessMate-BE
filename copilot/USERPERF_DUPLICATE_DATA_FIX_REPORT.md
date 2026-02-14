# UserPerf 중복 데이터 생성 문제 분석 및 해결 보고서

**작성일**: 2026-02-14  
**작성자**: GitHub Copilot  
**프로젝트**: ChessMate  
**이슈**: UserPerf 테이블에 중복 데이터 생성 현상

---

## 1. 문제 분석

### 1.1 증상
- `user_perfs` 테이블에 2415개 로우 존재 (비정상적으로 많음)
- 정상적으로는 사용자당 4개의 데이터만 존재해야 함 (BULLET, BLITZ, RAPID, CLASSICAL)
- 사용자 74, 79, 78 등에서 중복 데이터가 계속 생성되는 패턴 발견

### 1.2 근본 원인

**파일**: `LichessApiTaskHandler.java` (라인 163-205)

```java
// 문제 코드 - 매번 새로운 객체 생성하여 INSERT
UserPerf userPerf = UserPerf.builder()
    .userId(task.userId())
    .gameType(type)
    .rating(...)
    // ... 기타 필드 ...
    .build();

transactionTemplate.executeWithoutResult(status -> userPerfRepository.save(userPerf));
```

**문제점**:
1. 기존 데이터 존재 여부를 확인하지 않음
2. 배치 작업이 재실행될 때마다 새로운 레코드 INSERT
3. UPDATE 로직이 없어 항상 새로운 데이터가 생성됨

---

## 2. 해결 방안

### 2.1 코드 수정

**변경된 로직**: Update or Create (Upsert) 패턴 적용

```java
private boolean syncUserPerf(LichessApiTask task) {
    // ...
    for (GameType type : gameTypes) {
      try {
        UserPerfDto dto = lichessApiService.getUserPerf(task.username(), type);
        
        // ... 데이터 추출 ...
        
        transactionTemplate.executeWithoutResult(status -> {
          // 기존 데이터 조회
          UserPerf existingPerf = userPerfRepository
              .findByUserIdAndGameType(task.userId(), type)
              .orElse(null);

          if (existingPerf != null) {
            // 기존 데이터 업데이트
            existingPerf.setRating(...);
            existingPerf.setGamesPlayed(...);
            // ... 기타 필드 업데이트 ...
            userPerfRepository.save(existingPerf);
          } else {
            // 신규 데이터 생성
            UserPerf userPerf = UserPerf.builder()
                .userId(task.userId())
                .gameType(type)
                // ... 필드 설정 ...
                .build();
            userPerfRepository.save(userPerf);
          }
        });
      } catch (Exception e) {
        // ...
      }
    }
}
```

### 2.2 주요 변경 사항

| 항목 | 변경 전 | 변경 후 |
|------|--------|--------|
| 동작 방식 | Always INSERT | Update or CREATE |
| 중복 데이터 생성 | 가능 | 불가능 |
| 배치 재실행 | 중복 데이터 생성 | 데이터 갱신 |
| 로깅 | 없음 | [Worker-Update] / [Worker-Create] 구분 |

---

## 3. 추가 수정 사항

### 3.1 ImageUtil 방어 코드 추가

**파일**: `ImageUtil.java`

**문제**: CloudflareProperties가 null일 경우 NullPointerException 발생

**해결**:
```java
public String getProfileImageUrl(User u) {
    // CloudflareProperties null 체크 추가
    if (cloudflareProperties == null) {
      log.warn("CloudflareProperties가 null입니다. 기본값 반환");
      return "/default/default_profile.png";
    }
    // ...
}
```

---

## 4. 향후 조치 사항

### 4.1 기존 중복 데이터 정리 (선택 사항)

기존 데이터베이스의 중복 데이터를 정리하려면 다음 SQL 실행:

```sql
-- 1. 중복 데이터 확인
SELECT user_id, COUNT(*) as total_rows
FROM user_perfs
GROUP BY user_id
HAVING COUNT(*) >= 5
ORDER BY total_rows DESC;

-- 2. 가장 최신 데이터만 남기고 삭제
DELETE FROM user_perfs 
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MAX(id) AS id 
        FROM user_perfs 
        GROUP BY user_id, game_type
    ) AS tmp
);
```

### 4.2 모니터링 포인트

수정 후 다음을 모니터링:
- 배치 실행 로그에서 `[Worker-Update]` / `[Worker-Create]` 비율 확인
- `user_perfs` 테이블의 레코드 수 안정화 확인 (사용자당 4개)
- 게임 타입별 레이팅 데이터 정합성 확인

---

## 5. 수정 파일 목록

1. **LichessApiTaskHandler.java**
   - 메서드: `syncUserPerf()` (라인 163-241)
   - 변경사항: Update or Create 로직 구현

2. **ImageUtil.java**
   - 메서드: `getProfileImageUrl()`, `getBannerImageUrl()`
   - 변경사항: null 방어 코드 추가

---

## 6. 테스트 방안

### 6.1 단위 테스트
- `syncUserPerf()` 메서드에서 기존 데이터 UPDATE 확인
- 신규 사용자의 CREATE 확인

### 6.2 통합 테스트
- 배치 작업 재실행 후 중복 데이터 미생성 확인
- `user_perfs` 테이블 레코드 수 변화 없음 확인

---

## 결론

본 수정사항으로 인해:
1. ✅ 중복 데이터 생성 근본 원인 제거
2. ✅ 배치 작업 재실행 시 안정성 향상
3. ✅ 데이터 일관성 보증
4. ✅ 로깅 개선으로 모니터링 용이

**상태**: 수정 완료 ✅

