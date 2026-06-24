package com.studyflow.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 앱 기동 시 더미 데이터를 자동으로 삽입한다.
 *
 * <p>teacher1@studyflow.com 이 이미 존재하면 스킵하므로 멱등하게 동작한다.
 * SubjectDataInitializer(@Order(1)) 이후에 실행되어야 하므로 @Order(2).</p>
 *
 * <p>실행 순서:
 * <ol>
 *   <li>dummy_data.sql — 선생님 10명 + 학생 100명</li>
 *   <li>dummy_data_deleted.sql — 탈퇴 사용자 15명</li>
 *   <li>dummy_data_teacher_verification.sql — 선생님 인증 10건</li>
 *   <li>dummy_data_courses.sql — 수업 12개 (앱 기동으로 subject 시딩 후 실행됨)</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DummyDataInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    private static final String SENTINEL_EMAIL = "teacher1@studyflow.com";

    private static final String[] SCRIPTS = {
            "db/dummy/dummy_data.sql",
            "db/dummy/dummy_data_deleted.sql",
            "db/dummy/dummy_data_teacher_verification.sql",
            "db/dummy/dummy_data_courses.sql"
    };

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (alreadyLoaded(conn)) {
                log.info("[DummyDataInitializer] 더미 데이터가 이미 존재합니다. 스킵합니다.");
                return;
            }

            // DDL(ALTER TABLE)은 MySQL에서 암묵적 커밋을 유발하므로 트랜잭션 밖에서 먼저 실행.
            // voice_call_enabled 는 JPA ddl-auto 로 생성 시 DB 레벨 DEFAULT 가 없어서
            // INSERT 시 에러가 나므로 DEFAULT 를 미리 설정.
            setVoiceCallEnabledDefault(conn);

            // DML만 있는 스크립트를 트랜잭션으로 묶어 "전부 성공 or 전체 롤백" 보장
            conn.setAutoCommit(false);
            try {
                for (String scriptPath : SCRIPTS) {
                    ScriptUtils.executeSqlScript(conn, new ClassPathResource(scriptPath));
                    log.info("[DummyDataInitializer] 실행 완료: {}", scriptPath);
                }
                conn.commit();
                log.info("[DummyDataInitializer] 더미 데이터 삽입이 완료되었습니다. (총 {}개 스크립트)", SCRIPTS.length);
            } catch (Exception e) {
                conn.rollback();
                log.error("[DummyDataInitializer] 더미 데이터 삽입 실패, 롤백합니다.", e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void setVoiceCallEnabledDefault(Connection conn) {
        try (var st = conn.createStatement()) {
            st.execute("ALTER TABLE users MODIFY COLUMN voice_call_enabled TINYINT(1) NOT NULL DEFAULT 1");
            log.info("[DummyDataInitializer] voice_call_enabled DEFAULT 1 설정 완료");
        } catch (Exception e) {
            // 이미 DEFAULT 가 설정된 경우 무시
            log.debug("[DummyDataInitializer] voice_call_enabled DEFAULT 설정 스킵: {}", e.getMessage());
        }
    }

    private boolean alreadyLoaded(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE email = ?")) {
            ps.setString(1, SENTINEL_EMAIL);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
