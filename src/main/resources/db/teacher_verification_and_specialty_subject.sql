-- teacher_verification 컬럼 추가 + teacher_specialty_subject 조인 테이블 생성
-- 운영/스테이징 환경 배포 전 DBA 또는 배포 담당자가 직접 실행 (ddl-auto: validate 환경용)
-- 주의: 이 스크립트는 한 번만 실행해야 합니다. 재실행 시 중복 ALTER 오류 발생.
--       컬럼/테이블 존재 여부를 information_schema로 먼저 확인하거나, 별도 마이그레이션 도구(Flyway 등) 도입을 권장합니다.
-- 실행 대상: 운영 DB (studyflow_prod 또는 해당 스키마)

-- ── teacher_verification 신규 컬럼 ──────────────────────────────────────────
-- 인증 신청 시 제출하는 추가 정보 필드. 이전 버전에는 없던 컬럼들입니다.

-- MySQL은 ADD COLUMN IF NOT EXISTS를 지원하지 않으므로 실행 전 컬럼 존재 여부를 확인하세요.
-- 아래 쿼리로 사전 점검 가능:
--   SELECT column_name FROM information_schema.columns
--   WHERE table_schema = DATABASE() AND table_name = 'teacher_verification';

ALTER TABLE teacher_verification
    ADD COLUMN awards         TEXT         COMMENT '수상·특기사항',
    ADD COLUMN career         TEXT         COMMENT '대학교명 (컬럼명 career 유지)',
    ADD COLUMN major          VARCHAR(200) COMMENT '전공',
    ADD COLUMN admission_year VARCHAR(20)  COMMENT '학번 (예: 20학번)',
    ADD COLUMN rejected_reason TEXT        COMMENT '거절 사유 (REJECTED 상태일 때만 설정)',
    ADD COLUMN reviewed_at    DATETIME(6)  COMMENT '관리자 처리 일시 (PENDING이면 NULL)';

-- ── teacher_specialty_subject 조인 테이블 ────────────────────────────────────
-- TeacherProfile ↔ Subject ManyToMany 관계 테이블.
-- 선생님 전문 과목(최대 수능 8개 대분류) 다중 선택을 저장합니다.

CREATE TABLE IF NOT EXISTS teacher_specialty_subject
(
    teacher_profile_id BIGINT NOT NULL,
    subject_id         BIGINT NOT NULL,
    PRIMARY KEY (teacher_profile_id, subject_id),
    CONSTRAINT fk_tss_teacher_profile FOREIGN KEY (teacher_profile_id)
        REFERENCES teacher_profile (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tss_subject FOREIGN KEY (subject_id)
        REFERENCES subject (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tss_subject
    ON teacher_specialty_subject (subject_id);
