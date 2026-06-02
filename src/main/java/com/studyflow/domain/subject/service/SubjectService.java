package com.studyflow.domain.subject.service;

import com.studyflow.domain.subject.dto.response.SubjectResponse;
import com.studyflow.domain.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 과목 조회 서비스.
 *
 * <p>현재는 수능 8개 대분류(최상위 과목) 목록 조회를 제공한다.</p>
 */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    /**
     * 최상위 과목(= 수능 8개 대분류) 목록을 id 오름차순으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjects() {
        return subjectRepository.findByParentSubjectIsNullOrderByIdAsc().stream()
                .map(SubjectResponse::from)
                .toList();
    }
}
