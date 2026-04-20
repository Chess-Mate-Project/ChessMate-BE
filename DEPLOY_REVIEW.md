# ChessMate-BE 운영 배포 전 종합 점검 보고서

**점검 기준 브랜치:** `develop` (feat/#109 포함 최신)  
**점검 일시:** 2026-04-20  
**종합 판정: ⚠️ 조건부 배포 가능**

> R2 자격증명 rotate 완료 후 main merge 및 운영 배포 가능한 상태입니다.

---

## 목차

1. [보안 점검](#1-보안-점검)
2. [설정 및 환경 점검](#2-설정-및-환경-점검)
3. [기능 안전성 점검](#3-기능-안전성-점검)
4. [아키텍처 및 코드 품질](#4-아키텍처-및-코드-품질)
5. [종합 판정 및 액션 아이템](#5-종합-판정-및-액션-아이템)
6. [GitHub Secrets 등록 전체 목록](#6-github-secrets-등록-전체-목록)

---

## 1. 보안 점검

### 🔴 [CRITICAL] Cloudflare R2 실제 자격증명이 git에 추적됨

**파일:** `chessmate-api/src/main/resources/application-api.yml` (line 29–33)

```yaml
endpoint:  ${CLOUDFLARE_R2_ENDPOINT:https://8abcc1ccfb278ca2c5b3e8ba07623e9e.r2.cloudflarestorage.com}
accessKey: ${CLOUDFLARE_R2_ACCESS_KEY:599e880ffcface6a291030c9dbc2f0a1}
secretKey: ${CLOUDFLARE_R2_SECRET_KEY:06ee2c6f82726bb26b97cf2fabbfafd8224f7073fd0c87d68a3cf3383ee4d903}
cdn:       ${CLOUDFLARE_R2_CDN:https://pub-1e7b1be2d4da472a936752a572ed0419.r2.dev}
```

`.gitignore`에 `src/**/resources/application*.yml` 패턴이 있지만, 파일이 이미 git에 추적(`git ls-files`로 확인)되고 있어 gitignore가 무효한 상태입니다.

**위험 분석:**
- 레포지토리 접근 권한이 있는 누구나 R2 버킷 접근/파일 업로드/삭제 가능
- 운영 배포 시 docker-compose.prod.yml이 env var로 override하므로 **앱 런타임 동작은 안전**
- 그러나 자격증명은 이미 git history에 평문으로 존재

**필요 조치 (배포 전 필수):**

1. Cloudflare 대시보드에서 해당 R2 API 토큰 즉시 무효화
2. 새 API 토큰 발급
3. yml 파일을 git untrack 처리:

```bash
# 추적 중인 모든 application yml 파일 untrack
git rm --cached chessmate-api/src/main/resources/application-api.yml
git rm --cached chessmate-api/src/main/resources/application-prod.yml
git rm --cached chessmate-api/src/main/resources/application.yml
git rm --cached chessmate-common/src/main/resources/application-common.yml
git rm --cached chessmate-domain/src/main/resources/application-domain.yml
git rm --cached chessmate-external/src/main/resources/application-external-local.yml
git rm --cached chessmate-external/src/main/resources/application-external-prod.yml
git rm --cached chessmate-external/src/main/resources/application-external.yml
git rm --cached chessmate-external/src/main/resources/application.yml
git rm --cached chessmate-infra-persistence/src/main/resources/application-persistence.yml
git rm --cached chessmate-infra-redis/src/main/resources/application-redis.yml
git rm --cached chessmate-worker/src/main/resources/application-prod.yml
git rm --cached chessmate-worker/src/main/resources/application-worker.yml
git rm --cached chessmate-worker/src/main/resources/application.yml

git commit -m "chore: untrack yml files from git (credentials cleanup)"
```

4. 새 R2 자격증명을 GitHub Secrets에 등록 (6번 항목 참고)

---

### ✅ [MEDIUM → 수정 완료] `count()` 메서드 soft delete 필터링

**커밋:** `6fa544a`

`countByDeletedAtIsNull()`으로 교체 완료. `GET /api/user/count` 및 `/api/user/platform-stats`에서 탈퇴 유저가 집계에서 제외됩니다.

---

### ✅ JWT 인증

| 항목 | 판정 | 비고 |
|------|------|------|
| 서명 알고리즘 | 정상 | HS256 사용 |
| 만료 검증 | 정상 | `validateRefreshToken()` 구현 |
| Refresh Token Rotation | 정상 | Redis 서명 + 저장값 이중 검증 |
| 기본값 git 노출 | 주의 | `chessMateGbswProjectAccessTokenSecret` 등 git에 있음. 단, docker-compose.prod.yml이 env var로 완전히 override하므로 운영 배포 시 영향 없음 |

---

### ✅ OAuth2 (Lichess / Chess.com)

| 항목 | 판정 | 비고 |
|------|------|------|
| PKCE 구현 | 정상 | S256 표준 구현 |
| state 검증 | 정상 | Redis 저장 후 콜백 검증 + 즉시 삭제 |
| code_verifier TTL | 정상 | 100초 설정 |
| 재가입 restore | 정상 | `isDeleted()` → `restore()` → SyncJob enqueue |

---

### ✅ CORS / Security

| 항목 | 판정 | 비고 |
|------|------|------|
| 허용 오리진 | 정상 | localhost 2개, chessladder.org 2개로 제한 |
| CSRF | 정상 | Stateless JWT 구조상 비활성화 적절 |
| 공개 엔드포인트 | 정상 | `/api/user/platform-stats` 포함 최신화됨 |
| SQL Injection | 정상 | JPA 매개변수화 쿼리 사용 |

---

## 2. 설정 및 환경 점검

| 항목 | 파일 | 판정 | 근거 |
|------|------|------|------|
| DDL-AUTO | `application-prod.yml` | ✅ 정상 | `validate`로 override |
| Security 로깅 | `application-prod.yml` | ✅ 정상 | WARN 레벨로 override (개발 시 DEBUG/TRACE는 api 프로필에만 적용) |
| Batch 스키마 초기화 | `worker/application-prod.yml` | ✅ 정상 | `initialize-schema: never`로 override |
| Redis 인증 | `docker-compose.prod.yml` | ✅ 정상 | `requirepass` 적용 |
| R2 자격증명 (런타임) | `docker-compose.prod.yml` | ✅ 정상 | 모든 값 env var로 override |
| R2 자격증명 (git) | `application-api.yml` | 🔴 위험 | git history에 실제 값 존재 |
| Worker Job 자동 실행 | `application-worker.yml` | ✅ 정상 | `spring.batch.job.enabled: false` |
| Spring Profile 구성 | `docker-compose.prod.yml` | ✅ 정상 | API/Worker 각각 올바른 프로필 조합 |

### 운영 Spring Profile 구성

```
API 서버:    api, persistence, external-prod, common, domain, redis, prod
Worker 서버: worker, persistence, external-prod, common, domain, redis, prod
```

---

## 3. 기능 안전성 점검

### Soft Delete

| 항목 | 판정 | 비고 |
|------|------|------|
| `findAll()` | ✅ 수정됨 | `findAllByDeletedAtIsNull()` 적용 |
| `searchByUsernameContaining()` | ✅ 수정됨 | `AndDeletedAtIsNull` 조건 적용 |
| `count()` | ✅ 수정됨 | `countByDeletedAtIsNull()` 적용 |
| 재가입 restore | ✅ 정상 | `deletedAt = null` 초기화 + SyncJob enqueue |

### 인증 / 토큰 관리

| 항목 | 판정 | 비고 |
|------|------|------|
| 탈퇴 후 Redis 토큰 삭제 | ✅ 정상 | Access Token + Refresh Token 모두 삭제 |
| `clearCookies()` 실패 정책 | ✅ 수정됨 | `logout()`과 동일하게 예외 throw |
| 로그아웃 전략 패턴 | ✅ 정상 | Lichess/Chess.com 각각 구현 |

### Worker / 스케줄러

| 항목 | 판정 | 비고 |
|------|------|------|
| ScheduledSyncTrigger 탈퇴 유저 | ✅ 정상 | `findAll()` 수정으로 자동 제외 |
| SmartLifecycle 종료 처리 | ✅ 정상 | `stop()` 시 enqueue 차단 |
| Chess.com 병렬도 설정 | ✅ 정상 | `parallelism: 5` 설정 |
| Lichess Rate Limit | ✅ 정상 | RateLimiter 내장 |

### 트랜잭션

| 항목 | 판정 | 비고 |
|------|------|------|
| `deleteAccount()` | ✅ 정상 | `@Transactional` 적용 |
| `updateDescription()` | ✅ 정상 | `@Transactional` 적용 |
| OAuth callback | ✅ 정상 | `@Transactional` 적용 |
| 조회 메서드 | ✅ 정상 | `@Transactional(readOnly = true)` 적용 |

---

## 4. 아키텍처 및 코드 품질

### 멀티모듈 의존성 분리

```
chessmate-common
    ↑
chessmate-domain → common
chessmate-external → common
    ↑
chessmate-infra-persistence → domain, common
chessmate-infra-redis → common, external, domain
    ↑
chessmate-api / chessmate-worker → 위 모든 모듈
```

도메인 레이어가 인프라 구현에 의존하지 않는 구조로 분리가 올바릅니다.

### Repository 패턴

도메인 인터페이스 → JPA 구현체 분리가 일관되게 적용되어 있습니다.  
단, Lichess/Chess.com 양쪽에 동일한 패턴이 반복되어 있어 공통 추상화 여지가 있으나 현재 규모에서는 문제 없습니다.

### API 엔드포인트 일관성

| 엔드포인트 변경 | SecurityConfig | Controller | 판정 |
|----------------|----------------|------------|------|
| `/api/rank/platform-stats` → `/api/user/platform-stats` | ✅ 반영 | ✅ 반영 | 정상 |

---

## 5. 종합 판정 및 액션 아이템

### 판정 요약

```
🔴 CRITICAL  R2 자격증명 git 노출         → 배포 전 필수: 토큰 무효화 + rotate + untrack
✅ MEDIUM    count() soft delete 적용     → 수정 완료 (6fa544a)
✅ 나머지 전 항목                          → 배포 안전
```

### 배포 전 필수 액션 (순서대로)

- [ ] **1. R2 API 토큰 무효화**: Cloudflare 대시보드 → R2 → 관리 → API 토큰에서 현재 토큰 삭제
- [ ] **2. 새 R2 API 토큰 발급**: Access Key ID + Secret Access Key 재발급
- [ ] **3. yml 파일 git untrack**: 위 1번 항목의 `git rm --cached` 명령 실행 후 커밋
- [ ] **4. GitHub Secrets 전체 등록**: 아래 6번 항목 기준으로 23개 등록
- [ ] **5. main 브랜치 merge**: `develop → main` PR 생성 후 merge
- [ ] **6. GitHub Actions 배포 확인**: Actions 탭에서 워크플로우 성공 확인

### 배포 후 후속 액션

- [x] `count()` 메서드 soft delete 필터링 추가 (`countByDeletedAtIsNull`) — 완료
- [ ] EC2에서 `docker ps` 및 `docker logs` 로 컨테이너 상태 확인
- [ ] `GET /api/user/count` 응답으로 기본 동작 확인
- [ ] OAuth 로그인 플로우 E2E 확인

---

## 6. GitHub Secrets 등록 전체 목록

`GitHub Repository → Settings → Secrets and variables → Actions → New repository secret`

총 **23개** 등록 필요.

---

### 6-1. CI/CD 인프라 (4개)

| Secret 이름 | 형식 / 예시 | 설명 |
|-------------|------------|------|
| `DOCKER_USERNAME` | `myusername` | Docker Hub 사용자명 |
| `DOCKER_PASSWORD` | `dckr_pat_xxxxxx` | Docker Hub Access Token (패스워드 대신 토큰 권장) |
| `HOST_PROD` | `1.2.3.4` 또는 `ec2-xxx.compute.amazonaws.com` | EC2 IP 또는 도메인 |
| `PRIVATE_KEY` | `-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----` | EC2 SSH 키페어 전체 내용 |

---

### 6-2. 데이터베이스 (3개)

| Secret 이름 | 형식 / 예시 | 설명 |
|-------------|------------|------|
| `DB_URL` | `jdbc:mysql://<RDS_HOST>:3306/chess_mate?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8` | MySQL 접속 URL |
| `DB_USERNAME` | `chessmate` | MySQL 계정명 |
| `DB_PASSWORD` | (충분한 길이의 랜덤 문자열) | MySQL 패스워드 |

> `REDIS_HOST` / `REDIS_PORT`는 docker-compose 내부 서비스명(`redis`, `6379`)으로 고정되어 Secret 불필요

---

### 6-3. Redis (1개)

| Secret 이름 | 형식 / 예시 | 설명 |
|-------------|------------|------|
| `REDIS_PASSWORD` | (충분한 길이의 랜덤 문자열) | Redis `requirepass` 값 |

---

### 6-4. JWT (4개)

| Secret 이름 | 형식 / 예시 | 설명 |
|-------------|------------|------|
| `SPRING_JWT_ACCESS_TOKEN_SECRET` | 최소 32자 이상 랜덤 문자열 | Access Token 서명 키 |
| `SPRING_JWT_ACCESS_TOKEN_EXPIRATION` | `3600000` | 만료 시간 (ms, 기본 1시간) |
| `SPRING_JWT_REFRESH_TOKEN_SECRET` | 최소 32자 이상 랜덤 문자열 (Access와 다른 값) | Refresh Token 서명 키 |
| `SPRING_JWT_REFRESH_TOKEN_EXPIRATION` | `604800000` | 만료 시간 (ms, 기본 7일) |

---

### 6-5. Cloudflare R2 (5개)

> ⚠️ **반드시 신규 발급한 토큰으로 등록** (기존 값은 무효화 완료 후)

| Secret 이름 | 형식 / 예시 | 설명 |
|-------------|------------|------|
| `CLOUDFLARE_R2_ENDPOINT` | `https://<account_id>.r2.cloudflarestorage.com` | R2 스토리지 엔드포인트 |
| `CLOUDFLARE_R2_ACCESS_KEY` | (신규 발급 Access Key ID) | R2 Access Key ID |
| `CLOUDFLARE_R2_SECRET_KEY` | (신규 발급 Secret Access Key) | R2 Secret Access Key |
| `CLOUDFLARE_R2_BUCKET` | `chessladder` | 버킷명 |
| `CLOUDFLARE_R2_CDN` | `https://pub-xxxx.r2.dev` | CDN 퍼블릭 URL |

---

### 6-6. OAuth2 (4개)

| Secret 이름 | 형식 / 예시 | 설명 |
|-------------|------------|------|
| `LICHESS_CLIENT_ID` | `chessladder` | Lichess OAuth App Client ID |
| `LICHESS_REDIRECT_URL` | `https://chessladder.org/api/oauth/lichess/callback` | Lichess 콜백 URL |
| `CHESSCOM_CLIENT_ID` | (Chess.com 앱 Client ID) | Chess.com OAuth App Client ID |
| `CHESSCOM_REDIRECT_URL` | `https://chessladder.org/api/oauth/chesscom/callback` | Chess.com 콜백 URL |

---

### 6-7. 클라이언트 (2개)

| Secret 이름 | 형식 / 예시 | 설명 |
|-------------|------------|------|
| `CLIENT_URL` | `https://chessladder.org` | 프론트엔드 URL (CORS 허용 오리진) |
| `COOKIE_DOMAIN` | `chessladder.org` | 쿠키 도메인 (`.` 없이 등록) |

---

### 등록 완료 체크리스트

```
CI/CD 인프라
 [ ] DOCKER_USERNAME
 [ ] DOCKER_PASSWORD
 [ ] HOST_PROD
 [ ] PRIVATE_KEY

데이터베이스
 [ ] DB_URL
 [ ] DB_USERNAME
 [ ] DB_PASSWORD

Redis
 [ ] REDIS_PASSWORD

JWT
 [ ] SPRING_JWT_ACCESS_TOKEN_SECRET
 [ ] SPRING_JWT_ACCESS_TOKEN_EXPIRATION
 [ ] SPRING_JWT_REFRESH_TOKEN_SECRET
 [ ] SPRING_JWT_REFRESH_TOKEN_EXPIRATION

Cloudflare R2 (신규 발급 값으로)
 [ ] CLOUDFLARE_R2_ENDPOINT
 [ ] CLOUDFLARE_R2_ACCESS_KEY
 [ ] CLOUDFLARE_R2_SECRET_KEY
 [ ] CLOUDFLARE_R2_BUCKET
 [ ] CLOUDFLARE_R2_CDN

OAuth2
 [ ] LICHESS_CLIENT_ID
 [ ] LICHESS_REDIRECT_URL
 [ ] CHESSCOM_CLIENT_ID
 [ ] CHESSCOM_REDIRECT_URL

클라이언트
 [ ] CLIENT_URL
 [ ] COOKIE_DOMAIN
```
