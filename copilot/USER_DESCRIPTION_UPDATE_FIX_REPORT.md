# 사용자 자기소개 업데이트 기능 수정 보고서

## 문제 상황

사용자가 자기소개(description)를 수정 요청했을 때:
1. 요청이 **Content-Type: text/plain** 으로 전송됨
2. 백엔드는 **application/json** 형식의 `UpdateUserDescriptionRequest` Record를 기대함
3. **HttpMediaTypeNotSupportedException** 발생
4. 결과적으로 DB 저장이 이루어지지 않음

## 근본 원인

Spring의 `@RequestBody`는 기본적으로 `application/json` Content-Type을 기대합니다. 그러나 프론트에서 `text/plain`으로 요청하면 JSON 직렬화에 실패합니다.

```
원본 요청:
PUT /api/user/description
Content-Type: text/plain

"사용자가 입력한 자기소개"  <- String 타입으로 전송
```

```
백엔드 기대:
Content-Type: application/json

{
  "description": "사용자가 입력한 자기소개"
}
```

## 적용된 해결책

### 1. 컨트롤러 수정
**파일**: `UserController.java`

```java
// 변경 전
@PutMapping("/description")
public ResponseEntity<SuccessResponse<Void>> updateUserDescription(
    @AuthenticationPrincipal UserPrincipal u,
    @RequestBody UpdateUserDescriptionRequest request
)

// 변경 후
@PutMapping("/description")
public ResponseEntity<SuccessResponse<Void>> updateUserDescription(
    @AuthenticationPrincipal UserPrincipal u,
    @RequestBody String description
)
```

**변경 이유**:
- `String`으로 직접 받으면 `text/plain` 요청도 처리 가능
- 수신 후 `UpdateUserDescriptionRequest` 객체로 변환하여 서비스 호출

### 2. 서비스 로깅 강화
**파일**: `UserService.java`

#### updateUserDescription 메서드
- **변경 전**: 로그가 최소화되어 있어 저장 여부 확인 어려움
- **변경 후**: 다음 단계별 로깅 추가:
  1. `[자기소개 업데이트 시작]` - 요청 시작
  2. `[변경 전]` - 기존 값 확인
  3. `[메모리 변경 완료]` - 메모리상 변경 확인
  4. `[DB 저장 완료]` - DB 저장 완료 및 저장된 값 확인
  5. `[DB 재조회 확인]` - 저장 후 재조회하여 실제 저장 여부 재확인
  6. `[Cache-Invalidate]` - 캐시 무효화 완료

#### getUserProfile 메서드
- **변경 전**: 캐시 Hit/Miss 시 description 로깅 없음
- **변경 후**: 
  - `[Cache-Hit]`에 `cachedDescription` 포함하여 캐시된 값 확인
  - DB 조회 시 `description` 로깅으로 저장된 값 확인

### 3. 미사용 Import 제거
`DeleteMapping` import 제거

## 로그 흐름 예시

```log
[자기소개 업데이트 시작] userId=1, newDescription=안녕하세요
[변경 전] userId=1, oldDescription=null
[메모리 변경 완료] userId=1, newDescription=안녕하세요
[DB 저장 완료] userId=1, savedDescription=안녕하세요, 저장된 객체 id=1
[DB 재조회 확인] userId=1, DBDescription=안녕하세요
[Cache-Invalidate] UserProfile - userId=1, reason=description_updated

프로필 조회 요청:
[프로필 조회 시작] userId=1
[Cache-Miss] UserProfile - userId=1, DB 조회 시작
[DB 조회 완료] userId=1, dbDescription=안녕하세요, profileImage=null, bannerImage=null
[이미지 URL 생성 완료] userId=1, profileUrl=https://cdn/default/default_profile.png, bannerUrl=https://cdn/default/default_banner.png
[ProfileResponse 객체 생성 완료] userId=1, description=안녕하세요
[Cache-Set] UserProfile - userId=1, description=안녕하세요, TTL=3600s
[프로필 조회 완료] userId=1
```

## 프론트엔드 요청 방식

### 방법 1: application/json으로 요청 (권장)
```javascript
const updateDescription = async (description) => {
  const response = await fetch('/api/user/description', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      description: description
    })
  });
};
```

### 방법 2: text/plain으로 요청 (현재 코드에서 지원)
```javascript
const updateDescription = async (description) => {
  const response = await fetch('/api/user/description', {
    method: 'PUT',
    headers: {
      'Content-Type': 'text/plain',
      'Authorization': `Bearer ${accessToken}`
    },
    body: description
  });
};
```

## 캐시 무효화 동작

1. **자기소개 수정** -> `[Cache-Invalidate]` 로그 출력 -> 캐시 삭제
2. **프로필 조회** -> `[Cache-Miss]` 발생 (캐시 없음) -> DB 재조회
3. **새로운 데이터 캐싱** -> `[Cache-Set]` 로그 출력

이 과정에서 항상 최신 데이터가 조회됩니다.

## 검증 방법

1. **로그로 확인**
   ```
   [자기소개 업데이트 시작] userId=1, newDescription=테스트
   [DB 저장 완료] userId=1, savedDescription=테스트
   [Cache-Invalidate]
   ```

2. **DB 직접 확인**
   ```sql
   SELECT id, description FROM users WHERE id=1;
   ```

3. **프로필 API 호출**
   ```
   GET /api/user/profile
   응답의 description 필드에서 저장된 값 확인
   ```

## 수정된 파일 목록

1. `chessmate-api/src/main/java/com/chessmate/api/user/controller/UserController.java`
   - `@RequestBody String description` 으로 변경
   - 로깅 추가

2. `chessmate-api/src/main/java/com/chessmate/api/user/service/UserService.java`
   - updateUserDescription 메서드의 상세 로깅 추가
   - getUserProfile 메서드의 description 로깅 추가

## 예상 효과

- ✅ text/plain 요청 지원으로 프론트 호환성 향상
- ✅ 저장 여부를 로그로 명확히 추적 가능
- ✅ 캐시 무효화 동작 확인 가능
- ✅ 문제 발생 시 로그로 빠른 원인 파악 가능

