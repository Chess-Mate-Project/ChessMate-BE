# ChessMate 배포 아키텍처 설계

> 작성일: 2026-04-15  
> 목표: 최소 비용으로 EC2 단일 인스턴스 기반 운영 환경 구축

---

## 1. 현황 분석

### 프로젝트 구성
| 모듈 | 역할 | 포트 |
|------|------|------|
| `chessmate-api` | REST API 서버 (인증, 유저, 통계) | 8080 |
| `chessmate-worker` | 게임 데이터 수집 스케줄러 | 9000 |
| Redis | JWT refresh 토큰, 캐시 | 6379 |
| MySQL | 전체 데이터 영속 | 3306 |

### 현재 인프라 문제
- RDS 사용 → 최소 `db.t3.micro` 기준 약 **$15~20/월** 비용 발생
- 유저 데이터 140명(Lichess) 기존 RDS에 존재 → 신규 서버로 이전 필요

---

## 2. 목표 아키텍처

```
인터넷
  │
  ▼
[Nginx] (80/443)          EC2 단일 인스턴스
  │
  ├─ /api/*  → api:8080   [Docker: chessladder-api]
  │
  └─ (worker는 내부 전용)  [Docker: chessladder-worker]
                           [Docker: redis:7-alpine]
                           [Docker: mysql:8-alpine]
```

### EC2 인스턴스 선택

| 스펙 | t2.micro | t2.small |
|------|----------|----------|
| vCPU | 1 | 1 |
| RAM | 1 GB | 2 GB |
| 프리티어 | 12개월 무료 | 미해당 |
| 월 비용 (프리티어 이후) | ~$8.5 | ~$17 |

**권장: t2.small**  
이유: Spring Boot 컨테이너 2개(api + worker) + Redis + MySQL 동시 실행 시 1GB는 OOM 위험  
프리티어 기간 중에는 t2.micro로 시작하되, 아래 JVM 힙 튜닝 필수 적용

#### JVM 힙 설정 (t2.micro 운영 시)
```dockerfile
# dockerfile-api, dockerfile-worker에 ENTRYPOINT 수정
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]
```

---

## 3. docker-compose.prod.yml 수정

MySQL을 RDS 대신 컨테이너로 교체

```yaml
services:
  api:
    image: ${DOCKER_USERNAME}/chessladder-api:latest
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: api,persistence,external-prod,common,domain,redis,prod
      DB_URL: jdbc:mysql://mysql:3306/chessmate?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      SPRING_JWT_ACCESS_TOKEN_SECRET: ${SPRING_JWT_ACCESS_TOKEN_SECRET}
      SPRING_JWT_ACCESS_TOKEN_EXPIRATION: ${SPRING_JWT_ACCESS_TOKEN_EXPIRATION}
      SPRING_JWT_REFRESH_TOKEN_SECRET: ${SPRING_JWT_REFRESH_TOKEN_SECRET}
      SPRING_JWT_REFRESH_TOKEN_EXPIRATION: ${SPRING_JWT_REFRESH_TOKEN_EXPIRATION}
      CLOUDFLARE_R2_ENDPOINT: ${CLOUDFLARE_R2_ENDPOINT}
      CLOUDFLARE_R2_ACCESS_KEY: ${CLOUDFLARE_R2_ACCESS_KEY}
      CLOUDFLARE_R2_SECRET_KEY: ${CLOUDFLARE_R2_SECRET_KEY}
      CLOUDFLARE_R2_BUCKET: ${CLOUDFLARE_R2_BUCKET}
      CLOUDFLARE_R2_CDN: ${CLOUDFLARE_R2_CDN}
      CLIENT_URL: ${CLIENT_URL}
      COOKIE_DOMAIN: ${COOKIE_DOMAIN}
      LICHESS_CLIENT_ID: ${LICHESS_CLIENT_ID}
      LICHESS_REDIRECT_URL: ${LICHESS_REDIRECT_URL}
      CHESSCOM_CLIENT_ID: ${CHESSCOM_CLIENT_ID}
      CHESSCOM_REDIRECT_URL: ${CHESSCOM_REDIRECT_URL}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

  worker:
    image: ${DOCKER_USERNAME}/chessladder-worker:latest
    ports:
      - "9000:9000"
    environment:
      SPRING_PROFILES_ACTIVE: worker,persistence,external-prod,common,domain,redis,prod
      DB_URL: jdbc:mysql://mysql:3306/chessmate?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      LICHESS_CLIENT_ID: ${LICHESS_CLIENT_ID}
      LICHESS_REDIRECT_URL: ${LICHESS_REDIRECT_URL}
      CHESSCOM_CLIENT_ID: ${CHESSCOM_CLIENT_ID}
      CHESSCOM_REDIRECT_URL: ${CHESSCOM_REDIRECT_URL}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: chessmate
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    ports:
      - "3306:3306"          # 외부 노출은 마이그레이션 후 제거 권장
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10
    restart: unless-stopped
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --innodb-buffer-pool-size=128M

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  mysql_data:
```

> **MySQL 메모리 절약**: `--innodb-buffer-pool-size=128M` 설정으로 기본값(128MB~)을 명시 제한

---

## 4. GitHub Actions CI/CD 수정

`.github/workflows/deploy.yml` — DB_ROOT_PASSWORD 추가, 워크플로우명 변경

```yaml
name: Deploy to EC2

on:
  push:
    branches:
      - main

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew clean build -x test

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and Push Images
        run: |
          docker build -t ${{ secrets.DOCKER_USERNAME }}/chessladder-api:latest -f ./dockerfile-api .
          docker push ${{ secrets.DOCKER_USERNAME }}/chessladder-api:latest
          docker build -t ${{ secrets.DOCKER_USERNAME }}/chessladder-worker:latest -f ./dockerfile-worker .
          docker push ${{ secrets.DOCKER_USERNAME }}/chessladder-worker:latest

  deploy:
    runs-on: ubuntu-latest
    needs: build-and-push
    steps:
      - uses: actions/checkout@v4

      - name: Copy docker-compose.prod.yml to EC2
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.HOST_PROD }}
          username: ubuntu
          key: ${{ secrets.PRIVATE_KEY }}
          port: 22
          source: docker-compose.prod.yml
          target: /opt/chessladder

      - name: Deploy to EC2 via SSH
        uses: appleboy/ssh-action@v1.2.0
        with:
          host: ${{ secrets.HOST_PROD }}
          username: ubuntu
          key: ${{ secrets.PRIVATE_KEY }}
          port: 22
          script: |
            cd /opt/chessladder

            DOCKER_USERNAME="${{ secrets.DOCKER_USERNAME }}" \
            docker compose -f docker-compose.prod.yml down --remove-orphans

            DOCKER_USERNAME="${{ secrets.DOCKER_USERNAME }}" \
            docker compose -f docker-compose.prod.yml pull

            DOCKER_USERNAME="${{ secrets.DOCKER_USERNAME }}" \
            DB_URL="${{ secrets.DB_URL }}" \
            DB_USERNAME="${{ secrets.DB_USERNAME }}" \
            DB_PASSWORD="${{ secrets.DB_PASSWORD }}" \
            DB_ROOT_PASSWORD="${{ secrets.DB_ROOT_PASSWORD }}" \
            REDIS_PASSWORD="${{ secrets.REDIS_PASSWORD }}" \
            LICHESS_CLIENT_ID="${{ secrets.LICHESS_CLIENT_ID }}" \
            LICHESS_REDIRECT_URL="${{ secrets.LICHESS_REDIRECT_URL }}" \
            CHESSCOM_CLIENT_ID="${{ secrets.CHESSCOM_CLIENT_ID }}" \
            CHESSCOM_REDIRECT_URL="${{ secrets.CHESSCOM_REDIRECT_URL }}" \
            COOKIE_DOMAIN="${{ secrets.COOKIE_DOMAIN }}" \
            SPRING_JWT_ACCESS_TOKEN_SECRET="${{ secrets.SPRING_JWT_ACCESS_TOKEN_SECRET }}" \
            SPRING_JWT_ACCESS_TOKEN_EXPIRATION="${{ secrets.SPRING_JWT_ACCESS_TOKEN_EXPIRATION }}" \
            SPRING_JWT_REFRESH_TOKEN_SECRET="${{ secrets.SPRING_JWT_REFRESH_TOKEN_SECRET }}" \
            SPRING_JWT_REFRESH_TOKEN_EXPIRATION="${{ secrets.SPRING_JWT_REFRESH_TOKEN_EXPIRATION }}" \
            CLOUDFLARE_R2_ENDPOINT="${{ secrets.CLOUDFLARE_R2_ENDPOINT }}" \
            CLOUDFLARE_R2_ACCESS_KEY="${{ secrets.CLOUDFLARE_R2_ACCESS_KEY }}" \
            CLOUDFLARE_R2_SECRET_KEY="${{ secrets.CLOUDFLARE_R2_SECRET_KEY }}" \
            CLOUDFLARE_R2_BUCKET="${{ secrets.CLOUDFLARE_R2_BUCKET }}" \
            CLOUDFLARE_R2_CDN="${{ secrets.CLOUDFLARE_R2_CDN }}" \
            CLIENT_URL="${{ secrets.CLIENT_URL }}" \
            docker compose -f docker-compose.prod.yml up -d --force-recreate

            sudo nginx -t && sudo systemctl reload nginx
            docker image prune -f
            echo "Successfully Deployed!"
```

### GitHub Secrets 추가 필요 항목
기존 시크릿에서 **추가**되는 항목:
- `DB_ROOT_PASSWORD` — MySQL root 계정 패스워드

---

## 5. 데이터 마이그레이션 (기존 RDS Lichess 유저 140명)

### 전략: mysqldump → 신규 MySQL 컨테이너 import

#### Step 1. 기존 RDS에서 덤프 추출 (로컬 또는 구 EC2에서)
```bash
# Lichess 유저 테이블만 추출 (테이블명은 실제 스키마에 맞게 수정)
mysqldump \
  -h <RDS_ENDPOINT> \
  -u <DB_USERNAME> \
  -p<DB_PASSWORD> \
  chessmate \
  lichess_user \
  > lichess_user_dump.sql
```

#### Step 2. 덤프 파일을 신규 EC2로 전송
```bash
scp -i <키페어.pem> lichess_user_dump.sql ubuntu@<NEW_EC2_IP>:/tmp/
```

#### Step 3. 신규 MySQL 컨테이너에 import
```bash
# EC2 접속 후
docker exec -i $(docker ps -qf "name=mysql") \
  mysql -u<DB_USERNAME> -p<DB_PASSWORD> chessmate \
  < /tmp/lichess_user_dump.sql
```

#### Step 4. 확인
```bash
docker exec -it $(docker ps -qf "name=mysql") \
  mysql -u<DB_USERNAME> -p<DB_PASSWORD> chessmate \
  -e "SELECT COUNT(*) FROM lichess_user;"
# → 140 이상 확인
```

> **주의**: AUTO_INCREMENT 충돌 방지를 위해 dump에 `--no-tablespaces` 옵션을 추가하거나,  
> import 전 `SET FOREIGN_KEY_CHECKS=0;` 실행 필요 여부를 스키마 보고 판단

---

## 6. EC2 초기 셋업 체크리스트

신규 EC2(Ubuntu 22.04) 기준 1회성 작업:

```bash
# Docker 설치
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker ubuntu
newgrp docker

# Nginx 설치
sudo apt install -y nginx

# 디렉토리 생성
sudo mkdir -p /opt/chessladder
sudo chown ubuntu:ubuntu /opt/chessladder
```

Nginx 설정 (`/etc/nginx/sites-available/chessmate`):
```nginx
server {
    listen 80;
    server_name <도메인 또는 EC2 IP>;

    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/chessmate /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

---

## 7. 보안 그룹 설정 (EC2 Inbound Rules)

| 포트 | 프로토콜 | 허용 소스 | 용도 |
|------|----------|-----------|------|
| 22 | TCP | 개발자 IP만 | SSH |
| 80 | TCP | 0.0.0.0/0 | HTTP (Nginx) |
| 443 | TCP | 0.0.0.0/0 | HTTPS (SSL 적용 시) |
| 3306 | TCP | **차단** | MySQL - 외부 노출 불필요 |
| 6379 | TCP | **차단** | Redis - 외부 노출 불필요 |
| 8080 | TCP | **차단** | API - Nginx 통해서만 접근 |

---

## 8. 비용 예측

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| EC2 (t2.small) | - | 프리티어 12개월 무료 → 이후 ~$17/월 |
| RDS (db.t3.micro) | ~$15~20/월 | **제거** |
| EBS (30GB gp2) | ~$3/월 | 포함 (프리티어 30GB) |
| Cloudflare R2 | 무료 (10GB) | 유지 |
| **합계** | ~$18~23/월 | **프리티어 중 $0 → 이후 ~$17/월** |

---

## 9. 배포 순서 요약

1. 신규 EC2 생성 (t2.micro 또는 t2.small, Ubuntu 22.04)
2. EC2 초기 셋업 (Docker, Nginx 설치)
3. `docker-compose.prod.yml` 업데이트 (MySQL 컨테이너 추가)
4. GitHub Secrets에 `DB_ROOT_PASSWORD` 추가, `DB_URL` 값 변경 불필요 (컨테이너 내부 주소 사용)
5. `main` 브랜치 push → GitHub Actions 자동 빌드 & 배포
6. MySQL 컨테이너 기동 확인 후 데이터 마이그레이션 (Step 5 절차)
7. Nginx 설정 후 도메인 연결
8. 기존 RDS 스냅샷 보관 후 삭제