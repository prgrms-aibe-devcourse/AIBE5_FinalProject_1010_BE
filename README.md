# StudyFlow — Backend

<div align="center">
  <strong>선생님과 학생을 연결하는 양방향 비대면 강의 플랫폼</strong>
  <p>실시간 화상수업 · 공유 화이트보드 · AI 튜터 · QnA 등 상호작용 기반의 학습 환경을 제공합니다.</p>
</div>

---

## 📝 프로젝트 소개

> **"화면을 마주하는 순간, 배움의 거리는 사라집니다."**

선생님과 학생을 이어주는 비대면 학습 공간입니다. 일방향 영상 송출을 넘어 실시간 화상 강의,
공유 화이트보드, AI 튜터링을 결합한 상호작용 중심의 교육 솔루션을 지향합니다.

본 저장소는 인증, 수업/수강 관리, 결제 없는 매칭, 실시간 채팅·강의실, AI 질의응답(SSE 스트리밍),
파일 업로드(S3), 알림 등을 담당하는 **백엔드(Spring Boot)** 프로젝트입니다.

---

## 🛠 기술 스택

| 구분 | 기술 |
| :--- | :--- |
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.5.14 (Web, Data JPA, Security, Validation, WebSocket) |
| Build | Gradle |
| Database | MySQL 8 (운영), H2 (테스트) |
| ORM / Query | Spring Data JPA, QueryDSL 5.1 |
| Cache / Session | Redis |
| Auth | JWT (access/refresh), OAuth2 (Kakao · Google · Naver) |
| AI | Spring AI 1.0 (OpenAI `gpt-4o-mini`) — WebFlux 기반 SSE 스트리밍 |
| Realtime | Spring WebSocket(STOMP) 채팅, LiveKit 화상수업 |
| Storage | AWS S3 |
| Mail | Spring Mail (SMTP, 이메일 인증) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |

---

## 💡 주요 기능

- **인증/회원**: 이메일+비밀번호 로그인, 소셜 로그인(Kakao/Google/Naver), 이메일 인증, 비밀번호 재설정
- **선생님/학생**: 프로필 관리, 선생님 검색·노출 토글, 관리자 인증 심사
- **수업**: 수업 등록·수정·종료, 검색(키워드/과목/학년/방식/지역/가격/인원), 정렬(최신·오래된·가격·거리순)
- **수강**: 수강 신청·승인·중도 포기, 수강생 관리
- **강의실**: LiveKit 화상수업, 진도/공지/자유게시판/과제
- **AI 튜터**: 과목별 AI 질의응답 (동기 + SSE 스트리밍)
- **QnA 게시판**: 질문·답변·채택
- **알림 · 실시간 채팅(STOMP)**
- **관리자**: 통계, 선생님 인증 관리

---

## 📂 프로젝트 구조

```
src/main/java/com/studyflow/
├── StudyFlowApplication.java
├── domain/                  # 도메인별 패키지 (controller · service · repository · entity · dto)
│   ├── auth/                # 인증 (JWT, OAuth2, 이메일 인증)
│   ├── user/                # 사용자 공통
│   ├── teacher/             # 선생님 프로필 · 검색 · 인증
│   ├── student/             # 학생 프로필
│   ├── course/              # 수업 등록 · 검색 · 정렬
│   ├── enrollment/          # 수강 신청 · 수강 관리
│   ├── classroom/           # 화상수업 강의실 (LiveKit)
│   ├── ai/                  # AI 질의응답 (Spring AI, SSE)
│   ├── qna/                 # QnA 게시판
│   ├── chat/                # 실시간 채팅 (STOMP)
│   ├── assignment/          # 과제
│   ├── notification/        # 알림
│   ├── file/                # 파일 업로드 (S3)
│   ├── subject/             # 과목
│   ├── admin/               # 관리자 · 통계
│   └── naegong/             # (학습 관련 부가 도메인)
└── global/                  # 공통 인프라
    ├── config/              # Security, CORS, Swagger, WebClient 등 설정
    ├── auth/                # 인증 필터 · 토큰 처리
    ├── exception/           # ErrorCode, GlobalExceptionHandler
    ├── websocket/           # WebSocket(STOMP) 설정
    ├── redis/ · audit/ · util/
```

---

## 🚀 시작하기

### 사전 요구사항
- JDK 21
- Docker (로컬 MySQL · Redis 실행용)

### 1) 의존 인프라 실행 (MySQL + Redis)

```bash
# 비밀번호 등 환경변수 준비
cp .env-example .env        # MYSQL_ROOT_PASSWORD, REDIS_PASSWORD 채우기

docker compose up -d        # mysql(3306), redis(6379) 컨테이너 기동
```

### 2) 시크릿 설정

`src/main/resources/application.yml`은 환경변수/`application-secret.yml`에서 값을 주입받습니다.
git에 커밋되지 않는 `src/main/resources/application-secret.yml`을 만들어 최소한 OpenAI 키를 설정하세요.

```yaml
# application-secret.yml (예시)
spring:
  ai:
    openai:
      api-key: sk-...        # ⚠️ 없으면 앱이 기동되지 않음
```

> 그 밖의 비밀 값(`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_*`, `KAKAO/GOOGLE/NAVER_CLIENT_*`,
> `S3_BUCKET`, `AWS_*`, `LIVEKIT_*`, `REDIS_PASSWORD`)은 환경변수 또는 `application-secret.yml`로 주입합니다.
> 메일·OAuth·LiveKit·S3 기능을 쓰지 않는 경우 해당 설정만 비워두고 사용할 수 있습니다.

### 3) 애플리케이션 실행

```bash
./gradlew bootRun
```

- 서버: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- 스키마는 `ddl-auto: update`로 자동 반영됩니다 (별도 마이그레이션 스크립트 불필요).

### 테스트

```bash
./gradlew test          # H2 인메모리 DB 사용
```

---

## 🔑 주요 환경 변수

| 변수 | 설명 |
| :--- | :--- |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 접속 계정 |
| `REDIS_PASSWORD` | Redis 비밀번호 (로컬 무인증 시 빈 값) |
| `JWT_SECRET` | JWT 서명 키 |
| `SPRING_AI_OPENAI_API_KEY` | OpenAI API 키 (또는 `application-secret.yml`) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP 계정 (Gmail 앱 비밀번호) |
| `KAKAO_CLIENT_ID/SECRET` 외 | 소셜 로그인 (Google · Naver 포함) |
| `S3_BUCKET` / `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` | 파일 업로드 (S3, region: ap-northeast-2) |
| `LIVEKIT_API_KEY/SECRET/URL` | 화상수업 |
| `CORS_ALLOWED_ORIGINS` | 허용 오리진 (기본 `http://localhost:5173`) |

---

## 📦 배포

- **Dockerfile**: 멀티스테이지 빌드 (`eclipse-temurin:21`), `prod` 프로파일로 실행
- **docker-compose.prod.yml**: `app` + `mysql` + `redis` 구성 (EC2 배포용)

```bash
# EC2에서 (이미지/시크릿 환경변수 준비 후)
docker compose -f docker-compose.prod.yml up -d
```

---

## 📚 API 문서

- 실행 후 Swagger UI: `/swagger-ui/index.html`
- 명세 문서: 레포 루트의 [`API_명세서.md`](../API_명세서.md)

---

## 👥 팀 멤버

| 이름 (Github) | 역할 |
| :---: | :---: |
| **이재섭 (congsoony)** | 팀장 |
| **김현우 (gusdnzla26-art)** | FE / BE Developer |
| **이준영 (leejy1019)** | FE / BE Developer |
| **전우현 (jwh039)** | FE / BE Developer |

## 📅 프로젝트 기간

**2026년 5월 19일 (화) ~ 2026년 6월 26일 (금)** (약 6주)

> `AIBE5 데브코스 최종 프로젝트`
