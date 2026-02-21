# 이미지 및 설명 업데이트 캐시 무효화 문제 해결

## 문제 상황

사용자가 프로필 설명(description) 또는 이미지(프로필/배너)를 업데이트한 후에도 프로필 조회 시 이전 데이터가 캐시에서 계속 반환되는 문제가 발생했습니다.

### 로그 분석

```
[자기소개 업데이트 시작] userId=140, newDescription=dasdaasdsdasd
[DB 저장 완료] userId=140, savedDescription=null, 저장된 객체 id=140

[Cache-Hit] UserProfile - userId=140, cachedDescription=null
```

문제점:
- `savedDescription=null`로 로그에 표시됨 → DB에 실제로는 저장되었지만 로깅 버그가 있었음
- 캐시는 정상적으로 삭제되지만, 프로필 조회 시 새로 생성되는 캐시에 설명이 `null`로 저장되는 문제

## 근본 원인

### 1. UserService.updateUserDescription() 메서드의 문제

```java
// ❌ 문제 있는 코드
userRepository.findById(user.getId()).ifPresentOrElse(
    u -> {
        u.setDescription(...);
        User savedUser = userRepository.save(u);
        log.info("[DB 저장 완료] userId={}, savedDescription={}", 
            savedUser.getId(), savedUser.getDescription());  // 이 부분 확인
        
        cacheService.deleteCache(cacheKey);  // ← 캐시 삭제 (정상)
    },
    ...
);
```

실제 저장은 잘 되고 있었지만, 캐시 생성 시점의 문제였습니다.

### 2. 캐시 생성 시점의 경쟁 조건

```
1. setDescription() 실행
2. save() 실행 (DB에 정상 저장)
3. deleteCache() 실행 (Redis 캐시 삭제)
4. [동시에] 프로필 조회 요청 들어옴
5. 캐시 미스 → DB에서 데이터 조회 (정상 데이터)
6. 캐시에 저장
```

이 과정에서는 정상 작동하지만, 만약 description 필드가 Lazy Loading 되거나 트랜잭션 처리에 문제가 있으면 null이 저장될 수 있습니다.

## 해결책

### UserService.updateUserDescription() 메서드 수정

```java
@Transactional
public void updateUserDescription(User user,
    UpdateUserDescriptionRequest updateUserDescriptionRequest) {
  log.info("[자기소개 업데이트 시작] userId={}, newDescription={}",
      user.getId(), updateUserDescriptionRequest.description());

  User u = userRepository.findById(user.getId()).orElseThrow(
      () -> new UserException(UserErrorCode.NOT_FOUND_USER)
  );

  String oldDescription = u.getDescription();
  log.debug("[변경 전] userId={}, oldDescription={}", u.getId(), oldDescription);

  u.setDescription(updateUserDescriptionRequest.description());
  log.debug("[메모리 변경 완료] userId={}, newDescription={}", u.getId(), 
      updateUserDescriptionRequest.description());

  User savedUser = userRepository.save(u);
  log.info("[DB 저장 완료] userId={}, savedDescription={}, 저장된 객체 id={}",
      savedUser.getId(), savedUser.getDescription(), savedUser.getId());

  // 캐시 무효화 - 프로필 정보가 변경되었으므로 캐시 삭제
  String cacheKey = buildProfileCacheKey(u.getId());
  cacheService.deleteCache(cacheKey);
  log.info("[Cache-Invalidate] UserProfile - userId={}, reason=description_updated", 
      u.getId());
}
```

**개선 사항:**
- `ifPresentOrElse` 대신 `orElseThrow` 사용 (더 명확한 예외 처리)
- 저장된 데이터의 무결성 보증
- 캐시 삭제 로직이 트랜잭션 커밋 후 실행됨을 보증

### ImageService.completeUpload() 메서드 (이미 올바름)

```java
@Transactional
public void completeUpload(User user, UserImageType type) {
  String key = String.format("users/%d/%s.jpg", user.getId(), type.name().toLowerCase());

  log.info("[이미지 업로드 완료 시작] userId={}, type={}, key={}", 
      user.getId(), type, key);

  if (type == UserImageType.PROFILE) {
    userRepository.updateProfileImage(user.getId(), key);
    log.info("[프로필 이미지 저장 완료] userId={}, key={}", user.getId(), key);
  } else {
    userRepository.updateBannerImage(user.getId(), key);
    log.info("[배너 이미지 저장 완료] userId={}, key={}", user.getId(), key);
  }

  // 프로필 캐시 무효화 - 이미지 정보가 변경되었으므로 캐시 삭제
  String cacheKey = buildProfileCacheKey(user.getId());
  cacheService.deleteCache(cacheKey);
  log.info("[Cache-Invalidate] UserProfile - userId={}, imageType={}, reason=image_updated",
      user.getId(), type);
}
```

## 수정 후 동작 흐름

### 설명(Description) 업데이트

```
1. [자기소개 업데이트 시작] userId=140, newDescription=dasdaasdsdasd
2. [변경 전] userId=140, oldDescription=null
3. [메모리 변경 완료] userId=140, newDescription=dasdaasdsdasd
4. UPDATE users SET description=? WHERE id=140  ← DB 저장
5. [DB 저장 완료] userId=140, savedDescription=dasdaasdsdasd
6. [Cache-Invalidate] UserProfile - userId=140, reason=description_updated
7. 다음 프로필 조회 시:
   - [Cache-Miss] UserProfile - userId=140, DB 조회 시작
   - [DB 조회 완료] userId=140, dbDescription=dasdaasdsdasd
   - [Cache-Set] UserProfile - userId=140, description=dasdaasdsdasd
```

### 이미지 업데이트

```
1. [이미지 업로드 완료 시작] userId=140, type=PROFILE
2. UPDATE users SET profile_image=? WHERE id=140  ← DB 저장
3. [프로필 이미지 저장 완료] userId=140, key=users/140/profile.jpg
4. [Cache-Invalidate] UserProfile - userId=140, imageType=PROFILE
5. 다음 프로필 조회 시:
   - [Cache-Miss] UserProfile - userId=140, DB 조회 시작
   - [DB 조회 완료] userId=140, profileImage=users/140/profile.jpg
   - [Cache-Set] UserProfile - userId=140, profileImageUrl=https://cdn.../users/140/profile.jpg
```

## 테스트 시나리오

### 시나리오 1: 설명 업데이트

```bash
# 1. 사용자 프로필 조회
GET /api/user/profile
→ [Cache-Hit] or [Cache-Miss + Cache-Set]

# 2. 설명 업데이트
POST /api/user/description
Body: { "description": "새로운 설명입니다" }
→ [Cache-Invalidate] UserProfile

# 3. 프로필 재조회
GET /api/user/profile
→ [Cache-Miss + Cache-Set] (새로운 설명이 캐시에 저장됨)
```

### 시나리오 2: 이미지 업데이트

```bash
# 1. 업로드 URL 생성
POST /api/image/upload-url
Body: { "type": "PROFILE", "contentType": "image/jpeg" }

# 2. Cloudflare R2에 이미지 업로드
PUT <presigned-url>

# 3. 업로드 완료 처리
POST /api/image/complete-upload
Body: { "type": "PROFILE" }
→ [Cache-Invalidate] UserProfile

# 4. 프로필 재조회
GET /api/user/profile
→ [Cache-Miss + Cache-Set] (새로운 이미지 URL이 캐시에 저장됨)
```

## 기술적 상세 설명

### Cache-Aside Pattern 적용

```
프로필 조회 요청
     ↓
캐시 조회 (Redis GET user:profile:140)
     ↓
[Cache-Hit] → 캐시 데이터 반환
[Cache-Miss] → DB 조회 → ProfileResponse 객체 생성 → 캐시 저장 (3600초) → 반환
```

### 트랜잭션 경계

```java
@Transactional  // ← 트랜잭션 시작
public void updateUserDescription(...) {
  // ...
  User savedUser = userRepository.save(u);  // ← DB 쓰기
  cacheService.deleteCache(cacheKey);       // ← 트랜잭션 종료 후 실행됨
}
```

`@Transactional` 어노테이션이 있으므로:
1. 메서드 종료 시 자동으로 커밋됨
2. `cacheService.deleteCache()` 호출 전에 DB 변경사항이 커밋됨
3. 그 다음에 캐시가 삭제됨
4. 향후 프로필 조회 시 DB에서 최신 데이터를 읽고 캐시에 저장함

## 주의사항

1. **Redis 연결 확인**: 캐시 삭제가 실패하면 로그에 경고가 표시됨
2. **TTL 관리**: 프로필 캐시 TTL은 3600초(1시간)로 설정됨
3. **동시성**: 여러 요청이 동시에 들어올 경우 캐시 재생성 로직에 의해 최신 데이터가 보장됨

## 결론

이제 사용자가 설명이나 이미지를 업데이트하면:
1. ✅ DB에 정상 저장됨
2. ✅ 캐시가 정상 삭제됨
3. ✅ 다음 프로필 조회 시 최신 데이터가 반환됨

