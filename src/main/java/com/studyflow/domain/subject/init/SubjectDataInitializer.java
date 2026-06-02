package com.studyflow.domain.subject.init;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.enums.SubjectCategory;
import com.studyflow.domain.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 앱 기동 시 수능 8개 대분류 과목을 {@code subject} 테이블에 자동 시딩한다.
 *
 * <p>{@link SubjectCategory}의 각 값에 대해, 동일 분류의 과목이 아직 없을 때만 새로 만든다.
 * 이미 존재하면 건너뛰므로 서버를 몇 번 재기동해도 중복 생성되지 않는다(멱등).</p>
 *
 * <p>이렇게 DB에 행으로 두면 기존 Subject·Course 연관과 그대로 호환되고,
 * FE는 {@code GET /api/v1/subjects}로 과목 id를 받아 AI 질문에 사용할 수 있다.</p>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SubjectDataInitializer implements ApplicationRunner {

    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        for (SubjectCategory category : SubjectCategory.values()) {
            if (!subjectRepository.existsByCategory(category)) {
                subjectRepository.save(Subject.ofCategory(category));
                created++;
            }
        }
        if (created > 0) {
            log.info("[SubjectDataInitializer] 수능 대분류 과목 {}개를 시딩했습니다.", created);
        }
    }
}
