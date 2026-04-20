# 회원 탈퇴 설계 문서

## 개요

회원 탈퇴는 **Soft Delete** 방식으로 구현됩니다.  
유저 레코드를 물리적으로 삭제하지 않고 `deleted_at` 타임스탬프를 설정하여 탈퇴 처리합니다.

---

## 설계 결정

### Soft Delete를 선택한 이유

| 항목 | Hard Delete | Soft Delete |
|---|---|---|
| 동일 플랫폼 계정으로 재가입 탐지 | 별도 블랙리스트 테이블 필요 | `deleted_at` 필드만으로 탐지 가능 |
| 재가입 시 이전 레이팅 복원 | 불가 | 가능 (UserPerfStat 보존) |
| 실수 탈퇴 CS 대응 | 불가 | 계정 복원 가능 |
| game history 데이터 정합성 | orphan 발생 | userId 참조 유지 |

---

## 탈퇴 처리 내용

### DB
- `lichess_users` / `chesscom_users` 테이블
  - `deleted_at` = 현재 시각
  - `username` = `"deleted_{userId}"` (익명화)
  - `description`, `banner`, `profile` = `null` (개인정보 제거)

### Redis (모든 토큰 즉시 삭제)
| 플랫폼 | 삭제 키 |
|---|---|
| LICHESS | `lichess:accesstoken:{userId}`, `auth:refresh:LICHESS:{userId}` |
| CHESSCOM | `chesscom:accesstoken:{userId}`, `chesscom:refreshtoken:{userId}`, `auth:refresh:CHESSCOM:{userId}` |

### HTTP
- 인증 쿠키 (ACCESS, REFRESH) 즉시 만료 처리

### 보존 데이터
- `UserPerfStat` (레이팅/승무패 통계) — **유지**
- Game history / 집계 테이블 — **유지**

---

## 재가입(계정 복원) 동작

동일한 Lichess/Chess.com 플랫폼 계정으로 OAuth 로그인 시:

1. `findByLichessId()` / `findByChesscomId()` 로 기존 레코드 조회
2. `deletedAt != null` 이면 복원 처리
   - `deleted_at` = `null` 초기화
   - `username` = OAuth에서 받은 최신 유저명으로 업데이트
3. SyncJob 재등록 (탈퇴 기간 동안의 게임 통계 갱신)
4. 기존 `UserPerfStat` 데이터는 그대로 유지 (레이팅 리셋 불가)

---

## API Spec

### 회원 탈퇴

```
DELETE /api/user
Authorization: Bearer {accessToken}
```

**Request:** 파라미터 없음

**Response 200 OK:**
```json
{
  "message": "회원 탈퇴 성공",
  "data": null
}
```

---

## 변경 파일 목록

| 레이어 | 파일 | 변경 내용 |
|---|---|---|
| Domain | `LichessUser.java` | `deletedAt`, `softDelete()`, `restore()` 추가 |
| Domain | `ChesscomUser.java` | 동일 |
| Entity | `LichessUserEntity.java` | `deleted_at` 컬럼 추가 |
| Entity | `ChesscomUserEntity.java` | 동일 |
| Mapper | `LichessUserMapper.java` | `deletedAt` 매핑 추가 |
| Mapper | `ChesscomUserMapper.java` | 동일 |
| Service | `UserService.java` | `deleteAccount()` 추가 |
| Service | `AuthService.java` | `clearCookies()` 추가 |
| OAuth | `LichessOAuthService.java` | 탈퇴 계정 복원 로직 추가 |
| OAuth | `ChesscomOAuthService.java` | 동일 |
| Controller | `UserController.java` | `DELETE /api/user` 엔드포인트 추가 |
