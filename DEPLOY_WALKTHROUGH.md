# StudyFlow 배포 전 과정 기록 (2026-06-05)

> 이 문서는 실제 첫 배포를 수행한 **전 과정의 상세 기록**이다. 처음부터 따라하면 동일한 환경을 재구축할 수 있다.
> 운영 절차(중단/재개/철거/재구축)는 `DEPLOY.md` 참고. 이 문서는 "어떻게 만들었는가"의 기록이다.

## 0. 최종 아키텍처

```
[개발자] ──push──> GitHub (develop 또는 chore/deploy-setup)
                      │
        ┌─────────────┴──────────────┐
        ▼ (BE repo Actions)           ▼ (FE repo Actions)
  Docker 빌드 → GHCR 푸시        npm build (VITE_API_BASE 주입)
  .env 렌더 → scp → ssh 배포     → S3 sync → CloudFront 무효화
        │                             │
        ▼                             ▼
┌─ EC2 (Team10_1010) ─────────┐   S3: studyflow-frontend-team10
│ Caddy (80/443, HTTPS 자동)  │   CloudFront: d355xfn1ixngze.cloudfront.net
│  ├→ app:8080 (Spring Boot)  │         ▲
│  └→ CloudFront 프록시 ──────┼─────────┘
│ MySQL 8.0 / Redis (도커)    │
└──────────────────────────────┘

사용자 접속:
  사이트  https://studyflow1010.duckdns.org  (Caddy가 CloudFront로 중계)
  API     https://studyflow-api.duckdns.org  (Caddy가 Spring Boot로 중계)
```

**왜 이런 구조인가**
- HTTPS 필수: CloudFront(FE)가 https라 http API 호출은 브라우저가 Mixed Content로 차단. refresh token 쿠키도 `SameSite=None; Secure`라 HTTPS 필수.
- 도메인: 무료 DuckDNS 사용. CloudFront는 커스텀 도메인에 ACM 인증서가 필요한데 DuckDNS로는 ACM 검증이 불가 → **Caddy가 사이트 도메인의 HTTPS를 받아 CloudFront로 프록시**하는 방식 채택.
- MySQL/Redis를 RDS 대신 EC2 도커로: 비용 절약 (공유 계정, 테스트 단계).

---

## 1. 배포를 위해 변경/추가한 코드 (BE repo)

| 파일 | 신규/수정 | 내용 |
|---|---|---|
| `src/main/resources/application-prod.yml` | 신규 | **prod 프로필 설정 (self-contained)**. 핵심: ① datasource를 `mysql:3306`(컨테이너명)으로 ② Redis host `redis-server` ③ 모든 시크릿 `${ENV}` 참조 ④ OAuth redirect-uri를 `${BACKEND_URL}/login/oauth2/code/{provider}`로 ⑤ `server.forward-headers-strategy: framework` (프록시 뒤 https 인식) ⑥ `cors.allowed-origins: ${FRONTEND_URL}`. **application.yml이 gitignore라 CI 빌드 jar에 없으므로 prod 파일이 모든 설정을 자체 포함해야 함** |
| `.gitignore` | 수정 | `application-*.yml` 차단 규칙에 예외 추가: `!src/main/resources/application-prod.yml`, `!.env.prod.example` (둘 다 비밀값 없이 ${ENV} 참조만 있어 커밋 안전) |
| `docker-compose.prod.yml` | 전면 수정 | 기존 골격(app+mysql)에 ① **caddy 서비스 추가** (80/443, Caddyfile·인증서 볼륨) ② **redis-server 추가** (requirepass+AOF) ③ app에 누락 env 12개 추가 (REDIS_PASSWORD, OPENAI, OAuth 6종, FRONTEND/BACKEND_URL 등) ④ 업로드 볼륨 `uploads-data:/app/uploads` (FileService가 로컬 디스크 저장) ⑤ mysql을 `127.0.0.1:3306`에만 바인딩 (Workbench SSH 터널용, 인터넷 비노출) ⑥ app의 외부 포트 제거 (Caddy만 노출) |
| `Caddyfile` | 신규 | 도메인 2개 블록: `{$BACKEND_DOMAIN}` → `app:8080`, `{$FRONTEND_DOMAIN}` → `https://{$FRONTEND_CDN}` (CloudFront 중계, `header_up Host` 필수). Let's Encrypt 발급/갱신 자동 |
| `.github/workflows/deploy.yml` | 신규 | CD 파이프라인. 아래 3장 참고 |
| `.env.prod.example` | 신규 | 환경변수 목록 문서 (실값 없음) |
| `DEPLOY.md` | 신규 | 운영 절차서 |

**FE repo 변경:**

| 파일 | 신규/수정 | 내용 |
|---|---|---|
| `.github/workflows/deploy.yml` | 신규 | `npm ci` → `npm run build`(VITE_API_BASE는 Secret 주입) → `aws s3 sync dist --delete` → `cloudfront create-invalidation`. FE 소스 코드는 **무변경** (API 주소가 이미 `src/api/config.js`에서 env 기반이었음) |

> 두 repo 모두 `chore/deploy-setup` 브랜치에서 작업. 워크플로우 트리거는 `[develop, chore/deploy-setup]` — **develop 머지 후 chore 브랜치는 트리거에서 제거할 것** (TODO 주석 있음).

---

## 2. AWS 리소스 구축

> ⚠️ 공유 계정(데브코스 전 팀). `Team10_1010`/`studyflow-*` 리소스만 만질 것.

### 2-1. EC2 (관리자 제공)
- `i-0347e57c3d9e685ab` "Team10_1010" — **t3.micro, Amazon Linux 2023**, 디스크 8GB, 키페어 `Team10_1010` (pem은 관리자에게 받음)
- 보안그룹 인바운드: `22 (0.0.0.0/0 — GitHub Actions SSH 배포 때문에 내IP 제한 불가)`, `80`, `443` ← 80/443은 콘솔에서 직접 추가했음
- **Elastic IP `43.201.117.205`** 할당 후 연결 (콘솔)

### 2-2. EC2 초기 세팅 (SSH 접속 후 1회)
```bash
# swap 2GB — t3.micro(RAM 1GB)에서 Spring+MySQL+Redis 돌리기 위해 필수
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker + Compose 플러그인 (Amazon Linux 2023은 dnf)
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -sSL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

mkdir -p ~/studyflow
```

### 2-3. S3 + CloudFront (AWS CLI로 구축 — 액세스 키 필요)
```bash
# 프론트 버킷 (퍼블릭 차단 유지!)
aws s3api create-bucket --bucket studyflow-frontend-team10 --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2
aws s3api put-public-access-block --bucket studyflow-frontend-team10 \
  --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# OAC (CloudFront만 버킷 접근 허용하는 장치) → ID 메모
aws cloudfront create-origin-access-control --origin-access-control-config \
  "Name=studyflow-frontend-oac,OriginAccessControlOriginType=s3,SigningBehavior=always,SigningProtocol=sigv4"

# CloudFront 배포 생성 (JSON 설정 파일 사용) — 핵심 설정:
#   Origin: studyflow-frontend-team10.s3.ap-northeast-2.amazonaws.com + 위 OAC ID
#   DefaultRootObject: index.html (HashRouter라 404 리라이트 불필요)
#   ViewerProtocolPolicy: redirect-to-https / CachePolicy: CachingOptimized
aws cloudfront create-distribution --distribution-config file://cf-dist-config.json
# → 생성된 Domain(dxxxx.cloudfront.net)과 Id(EXXXX), ARN 메모

# 버킷 정책: cloudfront.amazonaws.com 서비스 프린시펄에 s3:GetObject 허용
#   Condition: AWS:SourceArn = 위 배포 ARN
aws s3api put-bucket-policy --bucket studyflow-frontend-team10 --policy file://bucket-policy.json
```
- 결과물: CloudFront `EID9Z97EIJMCN` / `d355xfn1ixngze.cloudfront.net`, OAC `E3TFMTM47OPJN4`

### 2-4. DuckDNS (무료 도메인 — duckdns.org에서 브라우저로)
- `studyflow-api` → 43.201.117.205 (백엔드)
- `studyflow1010` → 43.201.117.205 (사이트 — 같은 IP! Caddy가 도메인 이름 보고 분기)

---

## 3. GitHub Secrets / Variables (CD의 설정 저장소)

**원칙: 시크릿의 단일 출처 = GitHub.** 배포할 때마다 워크플로우가 러너에서 `.env`를 렌더링해 scp로 서버에 덮어쓴다. 값 변경 = GitHub에서 수정 → Actions Re-run.

### BE repo (`AIBE5_FinalProject_1010_BE`)

| Secrets (값 비공개) | 용도 |
|---|---|
| `EC2_HOST` / `EC2_USER` / `EC2_SSH_KEY` | Actions가 EC2에 scp/ssh 하기 위함. HOST=Elastic IP, USER=`ec2-user`, KEY=pem 전문 |
| `DB_PASSWORD` / `REDIS_PASSWORD` / `JWT_SECRET` | DB·Redis 비밀번호, JWT 서명키 |
| `OPENAI_API_KEY` | AI 기능 |
| `KAKAO_CLIENT_ID/SECRET`, `GOOGLE_~`, `NAVER_~` (6개) | 소셜 로그인 |
| `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` | (현재 dummy — S3 업로드 전환 시 실값) |
| `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET` | (현재 dummy — 화상수업 켤 때 실값) |

| Variables (값 공개 — GitHub에서 다시 볼 수 있음) | 값 |
|---|---|
| `BACKEND_DOMAIN` / `BACKEND_URL` | studyflow-api.duckdns.org / https://~ |
| `FRONTEND_DOMAIN` / `FRONTEND_URL` | studyflow1010.duckdns.org / https://~ |
| `FRONTEND_CDN` | d355xfn1ixngze.cloudfront.net (Caddy 프록시 대상) |
| `DB_USERNAME` | root |
| `S3_BUCKET` | studyflow-uploads (미사용 예약) |
| `LIVEKIT_URL` | wss://dummy |

### FE repo (`AIBE5_FinalProject_1010_FE`) — Secrets 5개

| Secret | 용도 |
|---|---|
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | S3 업로드 + CloudFront 무효화 권한 |
| `S3_FRONTEND_BUCKET` | `studyflow-frontend-team10` |
| `CLOUDFRONT_DISTRIBUTION_ID` | `EID9Z97EIJMCN` |
| `VITE_API_BASE` | `https://studyflow-api.duckdns.org` (빌드 시 번들에 인라인됨) |

---

## 4. 배포 파이프라인 동작 (push 후 일어나는 일)

**BE:** ① Docker 멀티스테이지 빌드(Gradle, `-x test`) → ② GHCR push (`ghcr.io/prgrms-aibe-devcourse/aibe5_finalproject_1010_be`, GITHUB_TOKEN으로 인증) → ③ 러너에서 `.env` 렌더 → ④ scp로 `docker-compose.prod.yml`+`Caddyfile`+`.env`를 `/home/ec2-user/studyflow`에 복사 → ⑤ ssh로 `docker compose pull app && up -d && image prune`

**FE:** ① `npm ci && npm run build` (VITE_API_BASE 주입) → ② `aws s3 sync dist --delete` → ③ CloudFront `create-invalidation /*`

**검증 방법:**
```powershell
# 사이트 (200 기대)
Invoke-WebRequest https://studyflow1010.duckdns.org
# API (401 기대 = Spring Security 정상 동작)
Invoke-WebRequest https://studyflow-api.duckdns.org/api/v1/lectures
# 서버 컨테이너 4개 확인
ssh -i <pem> ec2-user@43.201.117.205 "docker ps"
```

---

## 5. 소셜 로그인 콘솔 등록 (코드 외 작업)

3사 모두 redirect URI 추가 (기존 localhost 항목은 유지 — 로컬 개발용):
```
https://studyflow-api.duckdns.org/login/oauth2/code/{kakao|google|naver}
```
- 카카오: 내 애플리케이션 → 카카오 로그인 → Redirect URI
- 구글: API 및 서비스 → 사용자 인증 정보 → OAuth 클라이언트 → 승인된 리디렉션 URI (+동의 화면이 "테스트"면 테스트 사용자 등록 필요)
- 네이버: 내 애플리케이션 → API 설정 → Callback URL

---

## 6. 시행착오 기록 (같은 함정에 빠지지 않기)

| 증상 | 원인 | 해결 |
|---|---|---|
| Actions scp 단계 `ssh: no key found` | pem을 **PowerShell 파이프**로 `gh secret set`에 넣으면 내용 손상 | **bash**에서 `tr -d '\r' < key.pem \| gh secret set EC2_SSH_KEY -R <repo>` |
| 배포가 10분간 멈춤 → 타임아웃 → 서버 전체 다운 | `appleboy/ssh-action`의 script에 **heredoc**(\<\<EOF) 사용 — 행 단위 실행이라 입력 대기로 영구 정지. t3.micro가 과부하로 SSH까지 불능 | 파일 생성은 **러너에서** 하고 scp로 전송. 서버는 `aws ec2 reboot-instances`로 복구 |
| SSH `Permission denied (publickey)` | ① OS가 Ubuntu가 아니라 **Amazon Linux** → 사용자명 `ec2-user` ② **Elastic IP가 다른 팀 인스턴스에 연결**돼 있었음 (공유 계정 — 인스턴스 ID 꼭 확인!) | 사용자명 교정 + EIP를 올바른 인스턴스로 재연결 |
| `REMOTE HOST IDENTIFICATION HAS CHANGED` | EIP 재연결로 같은 IP가 다른 서버를 가리킴 | `ssh-keygen -R <IP>` 후 재접속 (정상 상황) |
| IAM 액세스 키 발급 불가 (explicit deny) | 데브코스가 셀프 발급 차단 | 관리자에게 키 요청 |
| Workbench 직접 접속 불가 | 3306을 인터넷에 안 열었음 (보안) | 연결 방식 **Standard TCP/IP over SSH** (pem + ec2-user, MySQL host 127.0.0.1) + compose에서 mysql을 `127.0.0.1:3306`에 바인딩 |
| 사이트는 뜨는데 API 호출 CORS 에러 | `FRONTEND_URL`(CORS 허용 목록)과 실제 접속 주소 불일치 | GitHub Variable `FRONTEND_URL` 수정 → 재배포 |
| DB 비밀번호 변경 시 | GitHub Secret만 바꾸면 안 됨 — MySQL 볼륨은 기존 비번 유지 | 서버에서 `ALTER USER` 먼저 → Secret 갱신 → 재배포 (순서 중요) |

## 7. 현재 상태 요약 (2026-06-05 기준)

- 운영 DB 비밀번호: `1234` (테스트 단계 합의 — 본운영 전 강한 값으로 변경 예정)
- LiveKit·S3업로드: dummy 키 (해당 기능 비활성)
- 파일 업로드: 로컬 디스크(도커 볼륨) — S3 전환은 코드 작업 필요
- 시연 후 **전부 철거 → t3.medium 재구축** 예정 (`DEPLOY.md` 6·7장)
