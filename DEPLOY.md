# StudyFlow 배포 가이드

```
[FE] develop push → GitHub Actions → S3(studyflow-frontend) → CloudFront(https://dxxxx.cloudfront.net)
[BE] develop push → GitHub Actions → GHCR 이미지 → EC2 SSH 배포
     EC2: Caddy(80/443, HTTPS 자동) → Spring Boot ─ MySQL / Redis (docker compose)
     백엔드 도메인: DuckDNS 무료 서브도메인 (예: studyflow-api.duckdns.org)
```

## 1. 사전 준비 (1회)

### 1-1. EC2
- 현재 운영 중: **Amazon Linux 2023, t3.micro** (관리자 제공, 키 페어 Team10_1010) + **Elastic IP**
- 보안그룹 인바운드: `22`, `80`, `443` — **8080/3306/6379는 열지 않는다**
  (22는 GitHub Actions가 SSH 배포에 사용하므로 0.0.0.0/0 필요)
- ⚠️ t3.micro는 RAM 1GB라 **swap 2GB 필수** (아래 1-3에 포함)

### 1-2. DuckDNS (백엔드 도메인)
1. https://www.duckdns.org 로그인(GitHub 계정 가능) → 서브도메인 생성 (예: `studyflow-api`)
2. current ip에 **Elastic IP** 입력 → update ip
3. `studyflow-api.duckdns.org` → Elastic IP 확인: `nslookup studyflow-api.duckdns.org`

### 1-3. EC2 초기 세팅 (Amazon Linux 2023 기준)
```bash
# swap 2GB (t3.micro 메모리 보강, 재부팅에도 유지)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker + Compose 플러그인 설치
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user   # 적용하려면 재로그인
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -sSL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 배포 디렉토리
mkdir -p ~/studyflow
```
- `~/studyflow/.env`는 **배포 워크플로우가 GitHub Secrets/Variables로부터 매번 자동 생성**한다 — 서버에서 직접 만들 필요 없음 (`.env.prod.example`은 항목 참고용)
- `docker-compose.prod.yml`, `Caddyfile`도 배포 때마다 자동 복사됨 (scp 단계)

### 1-4. GitHub Secrets / Variables (BE repo → Settings > Secrets and variables > Actions)

**시크릿/설정의 단일 출처는 GitHub.** 값을 바꾸려면 여기서 수정 후 Actions 재실행(또는 push).

| Secrets (비밀값) | Variables (공개 가능 설정) |
|---|---|
| `EC2_HOST` / `EC2_USER` / `EC2_SSH_KEY` (배포용) | `BACKEND_DOMAIN` / `BACKEND_URL` |
| `DB_PASSWORD` / `REDIS_PASSWORD` / `JWT_SECRET` | `FRONTEND_DOMAIN` / `FRONTEND_CDN` / `FRONTEND_URL` |
| `OPENAI_API_KEY` | `DB_USERNAME` |
| `KAKAO·GOOGLE·NAVER_CLIENT_ID/SECRET` (6개) | `S3_BUCKET` |
| `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` | `LIVEKIT_URL` |
| `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET` | |

- ⚠️ `EC2_SSH_KEY` 등록 시 PowerShell 파이프 금지(키 손상됨) — **bash에서** `tr -d '\r' < key.pem | gh secret set EC2_SSH_KEY -R <repo>`
- `JWT_SECRET` 새로 생성: `openssl rand -base64 32`

### 1-5. 소셜 로그인 콘솔 (카카오/구글/네이버)
- redirect URI 추가: `https://<백엔드도메인>/login/oauth2/code/{kakao|google|naver}`
- 기존 localhost URI는 그대로 두면 로컬 개발도 계속 가능

## 2. 백엔드 배포

- **자동**: develop에 push/merge되면 `.github/workflows/deploy.yml`이 빌드→배포
- **수동**: GitHub repo → Actions → "Deploy Backend to EC2" → Run workflow
- 확인: `https://<백엔드도메인>` 응답 확인, EC2에서 `docker compose -f docker-compose.prod.yml logs -f app`

> GHCR 첫 push 후 패키지가 private이면: GitHub org → Packages → 해당 이미지 → 권한 확인.
> org 정책으로 GITHUB_TOKEN의 패키지 쓰기가 막혀 있으면 PAT(write:packages)로 대체.

## 3. 프론트엔드 배포 (S3 + CloudFront)

### 3-1. AWS 리소스 (1회)
1. S3 버킷 `studyflow-frontend` 생성 — **퍼블릭 차단 유지** (정적 웹 호스팅 켜지 않음)
2. CloudFront 배포 생성:
   - Origin: 위 S3 버킷, **OAC(Origin Access Control)** 생성·연결 → 안내대로 버킷 정책 자동 추가
   - Default Root Object: `index.html`
   - 커스텀 도메인 없이 기본 `dxxxx.cloudfront.net` 사용 (ACM 불필요)
3. HashRouter라 403/404 리라이트 설정 불필요

### 3-2. FE repo GitHub Secrets
| Secret | 값 |
|---|---|
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | S3+CloudFront 권한 있는 IAM 키 |
| `S3_FRONTEND_BUCKET` | `studyflow-frontend` |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront 배포 ID |
| `VITE_API_BASE` | `https://<백엔드도메인>` |

- 이후 FE develop push마다 자동: build → s3 sync → CloudFront 캐시 무효화

## 4. 중단 / 재개 (비용 절약)

| 작업 | 방법 |
|---|---|
| 중단 | EC2 **Stop** (Terminate 금지 — 데이터 보존). 장기 중단이면 Elastic IP release(미연결 과금 ~$3.6/월) |
| 재개 | EC2 Start → (IP 바뀌었으면 Elastic IP 재연결 + DuckDNS ip 갱신) → 컨테이너는 `restart: unless-stopped`라 자동 기동 |
| S3/CloudFront | 트래픽 과금이라 그대로 둬도 사실상 $0 |

## 5. 리소스 인벤토리 (2026-06-05 기준)

> ⚠️ **공유 계정 주의**: 이 AWS 계정은 데브코스 전체 팀이 공유한다. **`Team10_1010` / `studyflow-*` 이름이 붙은 리소스만** 만질 것. (특히 `i-05c54a0b9f977b6c8`는 3팀 서버 — 절대 건드리지 말 것)

| 리소스 | 식별자 | 비고 |
|---|---|---|
| EC2 | `i-0347e57c3d9e685ab` (Team10_1010, t3.micro, AL2023) | 관리자 생성. Caddy+App+MySQL+Redis 도커 4컨테이너, swap 2GB |
| Elastic IP | `43.201.117.205` | DuckDNS 2개가 가리킴 |
| S3 | `studyflow-frontend-team10` | FE 정적 파일 (비공개+OAC) |
| CloudFront | `EID9Z97EIJMCN` (d355xfn1ixngze.cloudfront.net) | OAC: `E3TFMTM47OPJN4` |
| DuckDNS | `studyflow1010`(사이트), `studyflow-api`(API) | 무료, duckdns.org 계정에서 관리 |
| GHCR | `ghcr.io/prgrms-aibe-devcourse/aibe5_finalproject_1010_be` | 백엔드 이미지 |
| 미사용 | RDS / EB / ACM / ELB / Route53 | 대체: EC2내 Docker MySQL / Actions / Caddy·Let's Encrypt / Caddy / DuckDNS |

## 6. 전체 철거 절차 (예정: 시연 후 전부 삭제 → t3.medium으로 재구축)

1. (선택) DB 백업: `docker exec studyflow-mysql mysqldump -uroot -p1234 studyflow > backup.sql`
2. EC2 **Terminate** (`i-0347e57c3d9e685ab`) — EBS 루트 볼륨 자동 삭제, **DB 데이터 소멸**
3. Elastic IP **release** (43.201.117.205)
4. S3 비우고 삭제: `aws s3 rb s3://studyflow-frontend-team10 --force`
5. CloudFront: 배포 **비활성화 → (배포 완료 대기) → 삭제**, OAC 삭제
6. 남겨도 되는 것(무료): GitHub Secrets/Variables/워크플로우, DuckDNS 도메인, GHCR 이미지 — 재구축 때 재사용

## 7. 재구축 절차 (t3.medium 기준)

1. EC2 생성: **t3.medium**, AL2023(또는 Ubuntu — DEPLOY 1-3 명령 분기 주의), gp3 20GB, 보안그룹 22/80/443, 새 키페어 pem 보관
2. Elastic IP 할당·연결 → **DuckDNS 2개 도메인 IP 갱신** (duckdns.org)
3. GitHub Secret 갱신: `EC2_HOST`(새 IP), `EC2_SSH_KEY`(새 pem — bash에서 `tr -d '\r' < pem | gh secret set ...`)
4. 서버 초기화: 1-3 절차 (Docker/compose 설치, ~/studyflow 디렉토리. **medium이면 swap 생략 가능**)
5. S3/CloudFront 재생성(삭제했다면) → FE Secrets(`S3_FRONTEND_BUCKET`, `CLOUDFRONT_DISTRIBUTION_ID`) + BE Variable(`FRONTEND_CDN`) 갱신
6. BE/FE Actions **Re-run** → 사이트·API 복구 확인
7. (백업 떴다면) DB 복원: `docker exec -i studyflow-mysql mysql -uroot -p1234 studyflow < backup.sql`

## 8. 트러블슈팅

- **HTTPS 인증서 발급 실패**: DuckDNS가 Elastic IP를 정확히 가리키는지, 80/443이 열려 있는지 확인. `docker compose -f docker-compose.prod.yml logs caddy`
- **CORS 에러**: `.env`의 `FRONTEND_URL`이 CloudFront 주소와 정확히 일치하는지 (끝 슬래시 없이)
- **소셜 로그인 실패**: 콘솔 redirect URI ↔ `BACKEND_URL` 일치 확인
- **메모리 부족**: `free -h` 확인, 필요 시 swap 추가: `sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile`
- **디스크 부족**: `docker system df` 확인 → `docker image prune -a -f`
