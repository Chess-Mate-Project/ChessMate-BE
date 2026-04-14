# Streak API — currentStreak 필드 추가

## 변경 개요

`GET /api/stat/streak` 응답에 **현재 연속 플레이 일수(`currentStreak`)** 필드를 추가했습니다.

---

## 응답 구조 변경

### 변경 전

```json
[
    "year": 2024,
    "days": [...]
  },
  {
    "year": 2025,
    "days": [...]
  }
]
```

### 변경 후

```json
{
  "currentStreak": 5,
  "years": [
    {
      "year": 2024,
      "days": [...]
    },
    {
      "year": 2025,
      "days": [...]
    }
  ]
}
```

---

## currentStreak 계산 규칙

- 가장 최근 게임 날짜가 **오늘 또는 어제**인 경우에만 스트릭이 유효합니다.
- 그 날짜로부터 하루씩 거슬러 올라가며 **연속된 날의 수**를 셉니다.
- 가장 최근 게임이 2일 이상 전이면 `currentStreak: 0`을 반환합니다.
- `year` 파라미터 유무와 관계없이 **전체 데이터** 기준으로 계산합니다.

### 예시

| 플레이한 날짜 | currentStreak |
|---|---|
| 오늘, 어제, 그제 | 3 |
| 어제, 그제 | 2 |
| 오늘만 | 1 |
| 3일 전까지 (오늘/어제 없음) | 0 |

---

## 변경된 파일

| 파일 | 변경 내용 |
|---|---|
| `stat/dto/StreakResponse.java` | 신규 생성 — `currentStreak` + `years` 래퍼 DTO |
| `stat/service/StatService.java` | `getStreak()` 반환 타입 변경, `calculateCurrentStreak()` 추가 |
| `stat/controller/StatController.java` | 반환 타입 `List<YearlyGameStatResponse>` → `StreakResponse` 변경 |