package com.studyflow.domain.subject.repository;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.enums.SubjectCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 과목(subject) 리포지토리.
 *
 * <p>AI 질문에서 subjectId 유효성 검증 및 조회, 과목 목록 조회 API
 * (GET /api/v1/subjects), 그리고 8개 대분류 자동 시딩에 사용한다.</p>
 */
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /** 해당 대분류 과목이 이미 시딩되어 있는지 확인한다. (시딩 멱등성 보장용) */
    boolean existsByCategory(SubjectCategory category);

    /** 최상위(부모 없는) 과목 목록을 id 오름차순으로 조회한다. (= 8개 대분류) */
    List<Subject> findByParentSubjectIsNullOrderByIdAsc();
}
