-- course 테이블에 수업 방식(비대면/대면) 및 장소 컬럼 추가
-- 운영/스테이징 환경 배포 전 DBA 또는 배포 담당자가 직접 실행 (ddl-auto: validate 환경용)
-- 주의: 한 번만 실행. MySQL은 ADD COLUMN IF NOT EXISTS 미지원 — 먼저 아래 쿼리로 존재 여부 확인:
--   SELECT column_name FROM information_schema.columns
--   WHERE table_schema = DATABASE() AND table_name = 'course';

ALTER TABLE course
    ADD COLUMN teaching_mode  VARCHAR(10)  NOT NULL DEFAULT 'ONLINE' COMMENT '수업 방식: ONLINE(비대면) / OFFLINE(대면)',
    ADD COLUMN location       VARCHAR(300) COMMENT '대면 수업 장소 주소',
    ADD COLUMN location_lat   DOUBLE       COMMENT '위도',
    ADD COLUMN location_lng   DOUBLE       COMMENT '경도';
