# ChessMate-BE 배포 환경 설정 가이드

## 아키텍처 개요

```
로컬 개발
  application.yml
    └── SPRING_PROFILES_ACTIVE: api,persistence,external-local,...
  application-*.yml
    └── ${DB_URL:localhost기본값} → 로컬 DB/Redis 사용

프로덕션 (EC2 + Docker)
  GitHub Actions
    └── main 브랜치 push
    └── Gradle 빌드 → Docker 이미지 → Docker Hub
    └── EC2 SSH
          └── docker-compose.prod.yml 복사 (SCP)
          └── GitHub Secrets → env var 주입 → docker compose up

  EC2 컨테이너 3개
    ├── chessladder-api    (포트 8080)
    ├── chessladder-worker (포트 9000)
    └── redis:7-alpine     (포트 6379, 비밀번호 보호)
```

---

## 1. GitHub Secrets 설정

GitHub 레포 → **Settings → Secrets and variables → Actions → New repository secret**

### 전체 시크릿 목록

| Secret 이름 | 설명 | 예시 |
|---|---|---|
| `DOCKER_USERNAME` | Docker Hub 아이디 | `nuza` |
| `DOCKER_PASSWORD` | Docker Hub 비밀번호 또는 Access Token | |
| `HOST_PROD` | EC2 퍼블릭 IP | `13.xxx.xxx.xxx` |
| `PRIVATE_KEY` | EC2 SSH 개인키 (pem 파일 내용 전체) | `-----BEGIN RSA PRIVATE KEY-----...` |
| `DB_URL` | RDS 접속 URL | `jdbc:mysql://chessmate.xxxxx.ap-northeast-2.rds.amazonaws.com:3306/chess_mate?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | RDS 사용자명 | `admin` |
| `DB_PASSWORD` | RDS 비밀번호 | |
| `REDIS_PASSWORD` | Redis 비밀번호 | |
| `LICHESS_CLIENT_ID` | Lichess OAuth 앱 Client ID | `ChessLadder` |
| `LICHESS_REDIRECT_URL` | Lichess OAuth 리디렉트 URI | `https://api.chessladder.com/login/oauth2/code/lichess` |
| `CHESSCOM_CLIENT_ID` | Chess.com OAuth 앱 Client ID | `0cb23ccc-...` |
| `CHESSCOM_REDIRECT_URL` | Chess.com OAuth 리디렉트 URI | `https://api.chessladder.com/login/oauth2/code/chesscom` |
| `COOKIE_DOMAIN` | 쿠키 도메인 (프론트엔드와 동일 루트 도메인) | `chessladder.com` |
| `CLIENT_URL` | 프론트엔드 URL | `https://chessladder.com` |
| `SPRING_JWT_ACCESS_TOKEN_SECRET` | JWT 액세스 토큰 서명 키 (32자 이상 랜덤 문자열) | |
| `SPRING_JWT_ACCESS_TOKEN_EXPIRATION` | 액세스 토큰 만료 시간 (ms) | `3600000` (1시간) |
| `SPRING_JWT_REFRESH_TOKEN_SECRET` | JWT 리프레시 토큰 서명 키 (32자 이상 랜덤 문자열) | |
| `SPRING_JWT_REFRESH_TOKEN_EXPIRATION` | 리프레시 토큰 만료 시간 (ms) | `604800000` (7일) |
| `CLOUDFLARE_R2_ENDPOINT` | R2 S3 호환 엔드포인트 | `https://xxx.r2.cloudflarestorage.com` |
| `CLOUDFLARE_R2_ACCESS_KEY` | R2 Access Key ID | |
| `CLOUDFLARE_R2_SECRET_KEY` | R2 Secret Access Key | |
| `CLOUDFLARE_R2_BUCKET` | R2 버킷 이름 | `chessladder` |
| `CLOUDFLARE_R2_CDN` | R2 퍼블릭 CDN URL | `https://pub-xxx.r2.dev` |

> **신규 추가 필요 (기존에 없던 시크릿)**
> - `CHESSCOM_CLIENT_ID`
> - `CHESSCOM_REDIRECT_URL`
> - `COOKIE_DOMAIN`
>
> 이 3개가 없으면 배포 후 Chess.com OAuth와 쿠키 인증이 동작하지 않습니다.

---

## 2. EC2 초기 설정 (최초 1회)

```bash
# EC2 SSH 접속
ssh -i your-key.pem ubuntu@<EC2_IP>

# 배포 디렉토리 생성
sudo mkdir -p /opt/chessladder
sudo chown ubuntu:ubuntu /opt/chessladder

# Docker 설치 (미설치 시)
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2
sudo usermod -aG docker ubuntu
# 그룹 적용을 위해 재접속 필요

# Nginx 설치 (미설치 시)
sudo apt-get install -y nginx
```

### Nginx 설정

`/etc/nginx/sites-available/chessladder` 파일 생성:

```nginx
server {
    listen 80;
    server_name api.chessladder.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/chessladder /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

HTTPS 적용 (Let's Encrypt):

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.chessladder.com
```

---

## 3. 배포 흐름 (자동)

`main` 브랜치에 push하면 GitHub Actions가 자동 실행됩니다.

```
1. Gradle 빌드 (-x test)
2. Docker 이미지 빌드 & Docker Hub 푸시
   ├── {DOCKER_USERNAME}/chessladder-api:latest
   └── {DOCKER_USERNAME}/chessladder-worker:latest
3. EC2에 docker-compose.prod.yml 파일 복사 (SCP)
4. EC2 SSH 접속
   ├── 기존 컨테이너 중지 (down --remove-orphans)
   ├── 최신 이미지 pull
   ├── GitHub Secrets → env var → 컨테이너 시작 (up -d --force-recreate)
   └── Nginx 재시작
```

---

## 4. 로컬 개발 환경 설정

yml 파일은 `.gitignore` 대상이므로 팀원마다 로컬에 직접 파일을 보유해야 합니다.  
별도의 env var 설정 없이 아래 기본값으로 동작합니다.

| 항목 | 로컬 기본값 |
|---|---|
| DB | `localhost:3306` / user: `root` / pw: `1234` |
| Redis | `localhost:6379` / 비밀번호 없음 |
| JWT 시크릿 | 더미 값 (로컬 전용) |
| Cloudflare R2 | 빈 값 (이미지 업로드 미동작) |
| Cookie 도메인 | `localhost` |
| 클라이언트 URL | `http://localhost:5173` |

로컬에서 특정 env var를 오버라이드하고 싶다면 IntelliJ의 **Run Configuration → Environment Variables**에 추가하거나, `.env` 파일을 만들어 IDE에서 로드하면 됩니다.

---

## 5. 프로필 활성화 구조

| 환경 | API 활성 프로필 | Worker 활성 프로필 |
|---|---|---|
| **로컬** | `api, persistence, external-local, common, domain, redis` | `worker, persistence, external-local, common, domain, redis` |
| **프로덕션** | `api, persistence, external-prod, common, domain, redis, prod` | `worker, persistence, external-prod, common, domain, redis, prod` |

`prod` 프로필 활성화 시 추가 적용되는 설정:

| 모듈 | 설정 | 값 |
|---|---|---|
| API | `spring.jpa.hibernate.ddl-auto` | `validate` (스키마 자동 변경 차단) |
| API | 로그 레벨 | `INFO` / Security 관련 `WARN` |
| Worker | `spring.jpa.hibernate.ddl-auto` | `validate` |
| Worker | `spring.batch.jdbc.initialize-schema` | `never` (배치 테이블 보호) |

---

## 6. 트러블슈팅

### 컨테이너 로그 확인

```bash
docker logs chessladder-api-api-1 --tail=100
docker logs chessladder-api-worker-1 --tail=100
docker logs chessladder-api-redis-1 --tail=100
```

### 컨테이너 상태 확인

```bash
docker ps -a
```

### 환경변수 주입 여부 확인

```bash
docker exec chessladder-api-api-1 env | grep SPRING
docker exec chessladder-api-api-1 env | grep DB
```

### Redis 연결 테스트

```bash
docker exec chessladder-api-redis-1 redis-cli -a <REDIS_PASSWORD> ping
# PONG 이 출력되면 정상
```

### 수동 배포 (긴급 시)

```bash
ssh -i your-key.pem ubuntu@<EC2_IP>
cd /opt/chessladder

export DOCKER_USERNAME=...
export DB_URL=...
# (나머지 env var 동일하게 export)

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --force-recreate
```
