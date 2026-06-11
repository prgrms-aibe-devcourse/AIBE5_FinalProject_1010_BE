-- notification 테이블 생성 스크립트
-- 운영 환경 배포 전 DBA 또는 배포 담당자가 직접 실행 (ddl-auto: validate 환경용)
-- 실행 대상: 운영 DB (studyflow_prod 또는 해당 스키마)

CREATE TABLE IF NOT EXISTS notification
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT       NOT NULL,
    type         VARCHAR(40)  NOT NULL,
    title        VARCHAR(100) NOT NULL,
    message      TEXT,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    related_id   BIGINT,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient
    ON notification (recipient_id);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_read
    ON notification (recipient_id, is_read);
