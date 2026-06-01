package com.studyflow.domain.subject.repository;

import com.studyflow.domain.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 과목(subject) 리포지토리.
 *
 * <p>현재는 AI 질문에서 subjectId 유효성 검증 및 조회 용도로 사용한다.
 * 추후 과목 목록 조회 API(GET /api/v1/subjects)에서도 재사용할 수 있다.</p>
 */
public interface SubjectRepository extends JpaRepository<Subject, Long> {
}
