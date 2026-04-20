# ChessMate-BE 아키텍처 및 배포 가이드

> 최종 업데이트: 2026-04-20  
> 이 문서는 GUI 환경에서의 배포 작업을 위한 전체 시스템 참조 문서입니다.

---

## 목차

1. [전체 시스템 아키텍처](#1-전체-시스템-아키텍처)
2. [멀티모듈 구조](#2-멀티모듈-구조)
3. [기술 스택](#3-기술-스택)
4. [모듈별 상세 설명](#4-모듈별-상세-설명)
5. [환경변수 및 프로필 관리](#5-환경변수-및-프로필-관리)
6. [인증 및 보안](#6-인증-및-보안)
7. [외부 연동](#7-외부-연동)
8. [데이터베이스 및 캐시](#8-데이터베이스-및-캐시)
9. [Docker 및 배포 구성](#9-docker-및-배포-구성)
10. [CI/CD 파이프라인](#10-cicd-파이프라인)
11. [GitHub Secrets 목록](#11-github-secrets-목록)
12. [API 엔드포인트 정리](#12-api-엔드포인트-정리)
13. [배치 및 워커 아키텍처](#13-배치-및-워커-아키텍처)
14. [배포 체크리스트](#14-배포-체크리스트)

---

## 1. 전체 시스템 아키텍처

```
[ 클라이언트 (React + Vite) ]
         │
         ▼
[ Nginx (Reverse Proxy) ]  ← EC2 내부
         │
    ┌────┴────┐
    ▼         ▼
[ API 서버  ]  [ Worker 서버 ]
 port: 8080    port: 9000
    │               │
    └──────┬─────────┘
           │
    ┌──────┼──────┐
    ▼      ▼      ▼
[ MySQL ] [Redis] [Cloudflare R2]
  (RDS)   7-alpine  (이미지 저장)
           │
    ┌──────┴──────┐
    │             │
[ Lichess API ] [Chess.com API]
  (OAuth2 +      (OAuth2 +
   게임 수집)     게임 수집)
```

### 인프라 요약

| 구성 요소 | 역할 | 위치 |
|-----------|------|------|
| EC2 | 애플리케이션 실행 | AWS |
| RDS | MySQL 데이터 저장 | AWS |
| Redis | 캐시 + 토큰 저장 | EC2 내 Docker |
| Cloudflare R2 | 이미지 저장 (S3 호환) | Cloudflare |
| Nginx | 리버스 프록시 | EC2 내 |
| Docker Hub | 이미지 레지스트리 | Docker Hub |

---

## 2. 멀티모듈 구조

```
ChessMate-BE/
├── chessmate-api/           ← REST API 서버 (Boot App, port 8080)
├── chessmate-worker/        ← 배치 워커 서버 (Boot App, port 9000)
├── chessmate-domain/        ← 도메인 모델 + 레포지토리 인터페이스 (Library)
├── chessmate-common/        ← 공통 유틸리티, 상수 (Library)
├── chessmate-external/      ← 외부 API 통합 (WebFlux HTTP Client) (Library)
├── chessmate-infra-persistence/ ← JPA 구현체, MySQL 연동 (Library)
├── chessmate-infra-redis/   ← Redis 구현체, 캐시 서비스 (Library)
├── dockerfile-api           ← API 서버 Dockerfile
├── dockerfile-worker        ← Worker 서버 Dockerfile
├── docker-compose.prod.yml  ← 운영 Docker Compose
├── settings.gradle
└── build.gradle             ← 루트 빌드 설정
```

### 모듈 의존성 그래프

```
chessmate-common (독립)
       ↑
chessmate-domain → chessmate-common
       ↑
chessmate-external → chessmate-common
       ↑
chessmate-infra-persistence → chessmate-domain, chessmate-common
chessmate-infra-redis → chessmate-common, chessmate-external, chessmate-domain
       ↑
chessmate-api → 위 모든 모듈
chessmate-worker → 위 모든 모듈
```

### 모듈별 역할

| 모듈 | 종류 | 역할 |
|------|------|------|
| `chessmate-api` | Boot App | 사용자 요청 처리 REST API |
| `chessmate-worker` | Boot App | 게임 수집 배치, 통계 집계 |
| `chessmate-domain` | Library | 도메인 엔티티, 레포지토리 인터페이스 정의 |
| `chessmate-common` | Library | 공통 코드, 예외, 상수 |
| `chessmate-external` | Library | Lichess/Chess.com HTTP 클라이언트 |
| `chessmate-infra-persistence` | Library | JPA 구현체, 데이터베이스 레이어 |
| `chessmate-infra-redis` | Library | Redis 구현체, 캐시/토큰 레이어 |

---

## 3. 기술 스택

### 언어 및 프레임워크

| 항목 | 버전 |
|------|------|
| Java | 21 (Temurin, LTS) |
| Spring Boot | 3.5.0 |
| Spring Security | 6.x |
| Spring Batch | 3.5.0 |
| Spring WebFlux | 3.5.0 (HTTP Client 용도) |

### 데이터베이스 및 캐시

| 항목 | 버전/종류 | 용도 |
|------|----------|------|
| MySQL | 8.0+ | 메인 데이터베이스 (RDS) |
| Redis | 7-alpine | 토큰 캐시, OAuth 상태, 게임 큐 |
| Spring Data JPA + Hibernate | 3.5.0 | ORM |
| Lettuce | (내장) | Redis 클라이언트 |
| Apache Commons Pool2 | (최신) | Redis 연결 풀 |

### 인증

| 항목 | 버전 | 용도 |
|------|------|------|
| jjwt-api | 0.11.5 | JWT 생성/검증 |
| jjwt-impl | 0.11.5 | JWT 구현체 |
| jjwt-jackson | 0.11.5 | JWT Jackson 직렬화 |
| OAuth2 | Lichess, Chess.com | 소셜 로그인 |

### 인프라 및 배포

| 항목 | 용도 |
|------|------|
| Docker | 컨테이너화 |
| Docker Compose | 다중 컨테이너 오케스트레이션 |
| Docker Hub | 이미지 레지스트리 |
| GitHub Actions | CI/CD |
| AWS EC2 | 애플리케이션 서버 |
| AWS RDS | 관리형 MySQL |
| Nginx | 리버스 프록시 |
| Cloudflare R2 | S3 호환 오브젝트 스토리지 |
| AWS SDK S3 | 2.31.50, R2 연동 |

### 기타 라이브러리

| 라이브러리 | 버전 | 용도 |
|------------|------|------|
| Lombok | 최신 | 보일러플레이트 제거 |
| Jackson | 최신 | JSON 직렬화 |
| jackson-datatype-jsr310 | 최신 | LocalDateTime 직렬화 |
| Guava | 33.2.1-jre | 유틸리티 (Worker) |

---

## 4. 모듈별 상세 설명

### chessmate-api

REST API 서버. 사용자 인증, 프로필, 랭킹, 이미지 업로드, 동기화 상태 조회를 담당한다.

**주요 패키지 구조:**
```
com/chessmate/api/
├── global/
│   ├── auth/
│   │   ├── controller/     (AuthController)
│   │   ├── jwt/            (JwtService, JwtAuthenticationFilter, JwtGenerator)
│   │   ├── oauth/
│   │   │   ├── chesscom/   (ChesscomOauthController, ChesscomOAuthService)
│   │   │   ├── lichess/    (LichessOauthController, LichessOAuthService)
│   │   │   └── common/     (PlatFormOAuthController, CookieManager)
│   │   └── service/        (AuthService)
│   ├── config/             (SecurityConfig, CorsConfig)
│   └── exception/          (GlobalExceptionHandler)
├── user/                   (UserController, UserService)
├── rank/                   (RankController, RankService)
├── stat/                   (StatController, StatService)
├── image/                  (ImageController, Cloudflare R2 연동)
├── redis/                  (Redis 서비스)
└── sync/                   (SyncStatusController)
```

**활성 Spring Profile:** `api, persistence, external-prod, common, domain, redis, prod`

---

### chessmate-worker

배치 워커 서버. 외부 플랫폼(Lichess, Chess.com)에서 게임 데이터를 수집하고 통계를 집계한다.

**주요 클래스:**
```
com/chessmate/worker/
├── ChessmateWorkerApplication   (@EnableScheduling)
├── ChessComGameSyncWorker       (Chess.com 게임 동기화, 병렬도: 5)
├── LichessGameSyncWorker        (Lichess 게임 동기화, NDJSON 스트리밍)
├── GameStatAggregator           (색상별/타임클래스별/일별 통계 집계)
├── PerfStatFetcher              (성능 통계 API 조회)
├── ChesscomTokenRefresher       (OAuth Access Token 갱신)
├── SyncJobDispatcher            (작업 분배)
└── ScheduledSyncTrigger         (@Scheduled 정기 실행)
```

**활성 Spring Profile:** `worker, persistence, external-prod, common, domain, redis, prod`

---

### chessmate-domain

도메인 모델과 레포지토리 인터페이스만 정의. 인프라 구현에 의존하지 않는다.

**주요 엔티티:**
```
com/chessmate/domain/
├── game/
│   ├── Game                (게임 기록)
│   ├── GameResult          (Win/Loss/Draw)
│   └── MonthlyRating       (월별 레이팅)
├── lichess/user/
│   └── LichessUser         (Lichess 사용자)
├── chesscom/user/
│   └── ChesscomUser        (Chess.com 사용자)
├── stat/
│   ├── UserPerfStat        (타임클래스별 성능)
│   ├── UserColorStat       (백/흑 전적)
│   ├── UserDailyGameStat   (일일 게임 수)
│   └── UserFirstMoveStat   (초반 수 통계)
└── sync/
    ├── SyncJob             (동기화 작업 추적)
    └── SyncStatus          (PENDING / SUCCESS / FAILED)
```

---

### chessmate-external

Lichess, Chess.com API를 호출하는 HTTP 클라이언트 모듈. Spring WebFlux의 HTTP Interface(@HttpExchange)를 사용한다.

**주요 구조:**
```
com/chessmate/external/
├── api/
│   ├── lichess/LichessApi       (@HttpExchange, NDJSON 스트리밍 지원)
│   └── chesscom/ChesscomApi     (@HttpExchange)
├── config/
│   ├── RestClientConfig         (HTTP Interface 빈 등록)
│   ├── LichessProperties        (@ConfigurationProperties)
│   └── ChesscomProperties
└── dto/
    ├── account/                 (LichessAccountDto, PerfDto 등)
    └── chesscom/                (ChesscomGameResponse 등)
```

---

### chessmate-infra-persistence

도메인의 레포지토리 인터페이스를 JPA로 구현. 도메인 ↔ JPA Entity 매핑 처리.

**패턴:**
```
├── game/
│   ├── entity/GameJpaEntity          (JPA @Entity)
│   ├── jpaRepository/GameJpaRepository (Spring Data JPA)
│   ├── repositoryImpl/GameRepositoryImpl (도메인 인터페이스 구현)
│   ├── mapper/GameMapper              (도메인 ↔ JPA Entity 변환)
│   └── projection/MonthlyRatingProjection
```
(lichess, chesscom, stat 모두 동일 패턴)

---

### chessmate-infra-redis

Redis 연동 구현체. 토큰 저장, OAuth 상태, 게임 수집 큐 관리를 담당한다.

**Redis 키 프리픽스 체계:**

| 키 패턴 | 용도 |
|---------|------|
| `refresh_token:{userId}:{platform}` | JWT Refresh Token |
| `oauth:pkce:{state}` | OAuth PKCE 상태 값 |
| `oauth:lichess:token:{userId}` | Lichess OAuth Access Token |
| `user:playtime:{userId}` | 사용자 플레이타임 캐시 |
| `user:perfs:{userId}` | 사용자 성능 통계 캐시 |
| `user:playcount:{userId}` | 사용자 게임 수 캐시 |
| `auth:refresh:{userId}:{platform}` | 인증 정보 |

---

## 5. 환경변수 및 프로필 관리

### Spring Profile 활성화 방식

각 서버는 `SPRING_PROFILES_ACTIVE` 환경변수로 프로필을 지정한다.  
각 프로필은 `application-{profile}.yml` 파일을 로드한다.

**API 서버:**
```
SPRING_PROFILES_ACTIVE=api,persistence,external-prod,common,domain,redis,prod
```

**Worker 서버:**
```
SPRING_PROFILES_ACTIVE=worker,persistence,external-prod,common,domain,redis,prod
```

### 프로필별 설정 파일 위치

| 프로필 | 파일 위치 | 주요 내용 |
|--------|----------|-----------|
| `api` | `chessmate-api/src/main/resources/application-api.yml` | JWT 설정, 쿠키 도메인, 클라이언트 URL, Cloudflare R2 |
| `worker` | `chessmate-worker/src/main/resources/application-worker.yml` | Batch 설정, 병렬도 |
| `persistence` | `chessmate-infra-persistence/src/main/resources/application-persistence.yml` | MySQL, JPA/Hibernate |
| `redis` | `chessmate-infra-redis/src/main/resources/application-redis.yml` | Redis 호스트/포트/패스워드 |
| `external-prod` | `chessmate-external/src/main/resources/application-external-prod.yml` | Lichess/Chess.com OAuth URL, TTL |
| `prod` | (없음, 환경변수로 주입) | 운영 전용 시크릿 |

### 환경변수 목록 (docker-compose.prod.yml 주입)

**데이터베이스:**
```
DB_URL          = jdbc:mysql://<RDS_HOST>:3306/<DB_NAME>?serverTimezone=UTC&useSSL=true
DB_USERNAME     = <MySQL 사용자>
DB_PASSWORD     = <MySQL 패스워드>
```

**Redis:**
```
REDIS_PASSWORD  = <Redis 패스워드>
# REDIS_HOST, REDIS_PORT는 docker-compose 내부 서비스명으로 고정
```

**JWT:**
```
SPRING_JWT_ACCESS_TOKEN_SECRET      = <Access Token 서명 키>
SPRING_JWT_ACCESS_TOKEN_EXPIRATION  = 3600000 (ms, 기본 1시간)
SPRING_JWT_REFRESH_TOKEN_SECRET     = <Refresh Token 서명 키>
SPRING_JWT_REFRESH_TOKEN_EXPIRATION = 604800000 (ms, 기본 7일)
```

**OAuth2:**
```
LICHESS_CLIENT_ID       = <Lichess OAuth 클라이언트 ID>
LICHESS_REDIRECT_URL    = https://<도메인>/api/oauth/lichess/callback
CHESSCOM_CLIENT_ID      = <Chess.com OAuth 클라이언트 ID>
CHESSCOM_REDIRECT_URL   = https://<도메인>/api/oauth/chesscom/callback
```

**Cloudflare R2 (이미지 스토리지):**
```
CLOUDFLARE_R2_ENDPOINT  = https://<계정ID>.r2.cloudflarestorage.com
CLOUDFLARE_R2_ACCESS_KEY = <Access Key>
CLOUDFLARE_R2_SECRET_KEY = <Secret Key>
CLOUDFLARE_R2_BUCKET     = <버킷명>
CLOUDFLARE_R2_CDN        = https://<CDN 도메인>
```

**클라이언트:**
```
CLIENT_URL      = https://chessladder.org (또는 www)
COOKIE_DOMAIN   = chessladder.org
```

---

## 6. 인증 및 보안

### 인증 방식

- **JWT Stateless 인증**: 세션 미사용 (`SessionCreationPolicy.STATELESS`)
- **Access Token**: HTTP Authorization 헤더 또는 쿠키로 전달
- **Refresh Token**: Redis에 저장, 만료 시 재발급
- **OAuth2**: Lichess, Chess.com PKCE 방식

### 공개 엔드포인트 (인증 불필요)

```
OPTIONS /**                          (CORS Preflight)
GET  /api/oauth/oauth-url
GET  /api/oauth/callback
GET  /api/oauth/lichess/callback
GET  /api/oauth/chesscom/callback
GET  /api/auth/token
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/rank/ranking
GET  /api/user/count
GET  /api/user/platform-stats
GET  /login/oauth2/**
GET  /oauth2/authorization/lichess
GET  /oauth2/authorization/chesscom
GET  /login/oauth/code/chesscom
GET  /login/oauth/code/lichess
```

### CORS 허용 오리진

```
http://localhost:5173
http://localhost:5174
https://chessladder.org
https://www.chessladder.org
```

허용 메서드: `GET, POST, PUT, DELETE, PATCH, OPTIONS`  
자격증명(쿠키) 허용: `true`

### 로그아웃 전략 패턴

- `AbstractLogoutStrategy` 추상 클래스
- `LichessLogoutStrategy`: Lichess 토큰 무효화
- `ChesscomLogoutStrategy`: Chess.com 토큰 무효화

---

## 7. 외부 연동

### Lichess

| 항목 | 값 |
|------|-----|
| OAuth URL | `https://lichess.org/oauth` |
| Token URL | `https://lichess.org/api/token` |
| API Base | `https://lichess.org/api` |
| 사용 엔드포인트 | `/api/account`, `/api/user/{username}`, `/api/games/user/{username}` |
| 게임 응답 형식 | NDJSON 스트리밍 |
| TTL 캐시 | 36000초 |

### Chess.com

| 항목 | 값 |
|------|-----|
| OAuth URL | `https://oauth.chess.com/authorize` |
| Token URL | `https://oauth.chess.com/token` |
| API Base | `https://api.chess.com/pub` |
| 사용 엔드포인트 | `/player/{username}`, `/player/{username}/stats`, `/player/{username}/games/{year}/{month}` |
| TTL 캐시 | 36000초 |
| 동기화 병렬도 | 5 (설정 가능) |

### Cloudflare R2

AWS SDK S3 API와 호환. 사용자 프로필 이미지 저장에 사용.

---

## 8. 데이터베이스 및 캐시

### MySQL 주요 테이블

| 테이블 | 설명 |
|--------|------|
| `game` | 게임 기록 (`platform_game_id` 기준 중복 방지) |
| `lichess_user` | Lichess 사용자 정보 |
| `chesscom_user` | Chess.com 사용자 정보 |
| `user_perf_stat` | 타임클래스별 성능 통계 |
| `user_color_stat` | 백/흑 전적 통계 |
| `user_daily_game_stat` | 일별 게임 수 |
| `user_first_move_stat` | 초반 수 통계 |
| `sync_job` | 동기화 작업 추적 |
| `batch_*` | Spring Batch 메타데이터 |

### JPA DDL 전략

| 환경 | DDL Auto |
|------|----------|
| 로컬 | `update` |
| 운영 | `validate` |

### Redis 설정

| 항목 | 값 |
|------|-----|
| 이미지 | `redis:7-alpine` |
| 포트 | 6379 |
| 클라이언트 | Lettuce (내장) + Commons Pool2 |
| 직렬화 | 키: StringRedisSerializer, 값: GenericJackson2JsonRedisSerializer |

---

## 9. Docker 및 배포 구성

### dockerfile-api

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY chessmate-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### dockerfile-worker

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY chessmate-worker/build/libs/*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.prod.yml 구성

**서비스 3개:**

1. **api** (`chessladder-api:latest`)
   - 포트: `8080:8080`
   - Profile: `api,persistence,external-prod,common,domain,redis,prod`
   - Redis 서비스 의존

2. **worker** (`chessladder-worker:latest`)
   - 포트: `9000:9000`
   - Profile: `worker,persistence,external-prod,common,domain,redis,prod`
   - Redis 서비스 의존

3. **redis** (`redis:7-alpine`)
   - 포트: `6379:6379`
   - Healthcheck 포함
   - 패스워드 보호 (`--requirepass`)

**EC2 배포 경로:** `/opt/chessladder/`

---

## 10. CI/CD 파이프라인

### 트리거 조건

```yaml
on:
  push:
    branches:
      - main
```

`main` 브랜치에 push 시 자동 실행된다.

### 파이프라인 흐름

```
[Push to main]
      │
      ▼
[Job: build-and-push]
  1. Checkout (actions/checkout@v4)
  2. JDK 21 설정 (Temurin)
  3. Gradle 빌드 (테스트 제외)
     ./gradlew clean build -x test
  4. Docker Hub 로그인
  5. API 이미지 빌드 & Push
     → {DOCKER_USERNAME}/chessladder-api:latest
  6. Worker 이미지 빌드 & Push
     → {DOCKER_USERNAME}/chessladder-worker:latest
      │
      ▼
[Job: deploy] (build-and-push 완료 후 실행)
  1. Checkout
  2. docker-compose.prod.yml을 EC2로 SCP 전송
     → /opt/chessladder/
  3. SSH로 EC2 접속 후:
     a. 기존 컨테이너 중지 및 제거 (docker compose down --remove-orphans)
     b. 최신 이미지 풀 (docker compose pull)
     c. 환경변수 주입하여 컨테이너 시작 (docker compose up -d --force-recreate)
     d. Nginx 설정 검증 및 리로드
     e. 불필요 이미지 정리 (docker image prune -f)
      │
      ▼
  "Successfully Deployed!"
```

---

## 11. GitHub Secrets 목록

GitHub Repository → Settings → Secrets and variables → Actions 에서 관리.

### Docker Hub

| Secret 이름 | 설명 |
|-------------|------|
| `DOCKER_USERNAME` | Docker Hub 사용자명 |
| `DOCKER_PASSWORD` | Docker Hub 패스워드 또는 Access Token |

### EC2 접속

| Secret 이름 | 설명 |
|-------------|------|
| `HOST_PROD` | EC2 IP 또는 도메인 |
| `PRIVATE_KEY` | EC2 SSH 개인키 (PEM 전체 내용) |

### 데이터베이스

| Secret 이름 | 설명 |
|-------------|------|
| `DB_URL` | `jdbc:mysql://<host>:3306/<db>?serverTimezone=UTC&useSSL=true` |
| `DB_USERNAME` | MySQL 계정명 |
| `DB_PASSWORD` | MySQL 패스워드 |

### Redis

| Secret 이름 | 설명 |
|-------------|------|
| `REDIS_PASSWORD` | Redis requirepass 값 |

### JWT

| Secret 이름 | 설명 |
|-------------|------|
| `SPRING_JWT_ACCESS_TOKEN_SECRET` | Access Token 서명 키 (충분한 길이의 랜덤 문자열) |
| `SPRING_JWT_ACCESS_TOKEN_EXPIRATION` | ms 단위 (예: `3600000`) |
| `SPRING_JWT_REFRESH_TOKEN_SECRET` | Refresh Token 서명 키 |
| `SPRING_JWT_REFRESH_TOKEN_EXPIRATION` | ms 단위 (예: `604800000`) |

### OAuth2

| Secret 이름 | 설명 |
|-------------|------|
| `LICHESS_CLIENT_ID` | Lichess OAuth App Client ID |
| `LICHESS_REDIRECT_URL` | 예: `https://chessladder.org/api/oauth/lichess/callback` |
| `CHESSCOM_CLIENT_ID` | Chess.com OAuth App Client ID |
| `CHESSCOM_REDIRECT_URL` | 예: `https://chessladder.org/api/oauth/chesscom/callback` |

### Cloudflare R2

| Secret 이름 | 설명 |
|-------------|------|
| `CLOUDFLARE_R2_ENDPOINT` | 예: `https://<accountId>.r2.cloudflarestorage.com` |
| `CLOUDFLARE_R2_ACCESS_KEY` | R2 Access Key ID |
| `CLOUDFLARE_R2_SECRET_KEY` | R2 Secret Access Key |
| `CLOUDFLARE_R2_BUCKET` | 버킷 이름 |
| `CLOUDFLARE_R2_CDN` | CDN 퍼블릭 URL (예: `https://cdn.chessladder.org`) |

### 클라이언트

| Secret 이름 | 설명 |
|-------------|------|
| `CLIENT_URL` | 예: `https://chessladder.org` |
| `COOKIE_DOMAIN` | 예: `chessladder.org` |

---

## 12. API 엔드포인트 정리

### 인증 (`/api/auth`)

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|-----------|------|
| POST | `/api/auth/token` | 불필요 | 토큰 발급 |
| POST | `/api/auth/refresh` | 불필요 | Access Token 재발급 |
| POST | `/api/auth/logout` | 불필요 | 로그아웃 |

### OAuth2 (`/api/oauth`)

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|-----------|------|
| GET | `/api/oauth/oauth-url` | 불필요 | OAuth 로그인 URL 반환 |
| GET | `/api/oauth/callback` | 불필요 | OAuth 공통 콜백 |
| GET | `/api/oauth/lichess/callback` | 불필요 | Lichess 콜백 |
| GET | `/api/oauth/chesscom/callback` | 불필요 | Chess.com 콜백 |

### 사용자 (`/api/user`)

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|-----------|------|
| GET | `/api/user/profile` | 필요 | 내 프로필 조회 |
| GET | `/api/user/card` | 필요 | 카드 정보 조회 |
| PUT | `/api/user/description` | 필요 | 자기소개 수정 |
| DELETE | `/api/user` | 필요 | 회원 탈퇴 (soft delete) |
| GET | `/api/user/count` | 불필요 | 전체 유저 수 |
| GET | `/api/user/platform-stats` | 불필요 | 플랫폼별 유저 수 |

### 랭킹 (`/api/rank`)

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|-----------|------|
| GET | `/api/rank/ranking` | 불필요 | 랭킹 조회 |

### 이미지 (`/api/image`)

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|-----------|------|
| POST | `/api/image/upload` | 필요 | 프로필 이미지 업로드 (R2) |

### 동기화 (`/api/sync`)

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|-----------|------|
| GET | `/api/sync/status` | 필요 | 게임 동기화 상태 조회 |

---

## 13. 배치 및 워커 아키텍처

### 동기화 흐름

```
[ScheduledSyncTrigger] (@Scheduled, 정기 실행)
         │
         ▼
[SyncJobDispatcher] (작업 분배)
    ├─────────────────────────────────┐
    ▼                                 ▼
[LichessGameSyncWorker]    [ChessComGameSyncWorker]
  - NDJSON 스트리밍           - REST API 폴링
  - 증분 수집 지원             - 병렬도: 5
    │                           │
    └──────────┬─────────────────┘
               ▼
    [GameStatAggregator] (통계 집계)
    - 색상별 (백/흑)
    - 타임클래스별
    - 일별 게임 수
               │
               ▼
    [PerfStatFetcher] (성능 통계)
    - Lichess Perfs API
    - Chess.com Stats API
```

### Spring Batch vs @Scheduled

- **@Scheduled**: 경량 정기 실행 (동기화 트리거)
- **Spring Batch**: 대용량 배치 처리 (메타데이터 테이블 활용)
- Worker 서버에서만 작동, API 서버와 완전 분리

---

## 14. 배포 체크리스트

### 최초 배포 전 준비

- [ ] EC2 인스턴스 생성 및 Docker, Docker Compose, Nginx 설치
- [ ] RDS MySQL 인스턴스 생성 (8.0+)
- [ ] Cloudflare R2 버킷 생성 및 API 키 발급
- [ ] Lichess OAuth App 등록 및 Client ID 확인
- [ ] Chess.com OAuth App 등록 및 Client ID 확인
- [ ] Docker Hub 계정 생성 및 Access Token 발급
- [ ] EC2 SSH 키 페어 생성
- [ ] EC2 보안그룹에서 포트 80, 443, 22 허용
- [ ] `/opt/chessladder/` 디렉토리 생성 (`mkdir -p /opt/chessladder`)
- [ ] Nginx 설정 파일 작성 (8080, 9000 프록시)
- [ ] GitHub Secrets 전체 등록 (11번 항목 참고)

### 배포 실행

- [ ] `develop` → `main` 브랜치로 Merge 또는 Push
- [ ] GitHub Actions 탭에서 워크플로우 실행 확인
- [ ] build-and-push 잡 성공 확인
- [ ] deploy 잡 성공 및 `Successfully Deployed!` 로그 확인

### 배포 후 검증

- [ ] `https://<도메인>/api/user/count` 응답 확인
- [ ] `https://<도메인>/api/rank/ranking` 응답 확인
- [ ] OAuth 로그인 플로우 동작 확인
- [ ] EC2에서 `docker ps` 로 api, worker, redis 컨테이너 실행 상태 확인
- [ ] EC2에서 `docker logs chessladder-api` 로 에러 없음 확인
- [ ] EC2에서 `docker logs chessladder-worker` 로 에러 없음 확인

### EC2에서 유용한 명령어

```bash
# 컨테이너 상태 확인
docker ps

# API 서버 로그
docker logs -f chessladder-api

# Worker 서버 로그
docker logs -f chessladder-worker

# Redis 연결 확인
docker exec -it <redis-container> redis-cli -a <password> ping

# 재배포 없이 재시작
cd /opt/chessladder
docker compose -f docker-compose.prod.yml restart api

# Nginx 재로드
sudo systemctl reload nginx
```
