package com.studyflow.domain.teacher.dto;

import java.util.List;

/**
 * 선생님 찾기 페이지 검색/필터 조건.
 *
 * <p>컨트롤러가 쿼리 파라미터를 그대로 담아 서비스로 전달하면,
 * 서비스에서 만 나이 → 출생일 범위 변환, 정렬 방향 결정, 빈 목록 처리를 수행한다.</p>
 *
 * @param keyword      선생님 이름 부분 검색어 (null 가능)
 * @param gender       성별 "MALE"/"FEMALE" (null/그 외 값이면 무시)
 * @param minAge       최소 만 나이 (null이면 무시)
 * @param maxAge       최대 만 나이 (null이면 무시)
 * @param regions      활동 지역 목록 — 정확 일치 OR (null/빈 목록이면 무시)
 * @param universities 대학교 목록 — 정확 일치 OR (null/빈 목록이면 무시)
 * @param subjectIds   전문 과목 id 목록 — 하나라도 포함 시 노출 (null/빈 목록이면 무시)
 * @param sort         정렬 "LATEST"(최신순, 기본) / "OLDEST"(오래된순)
 */
public record TeacherSearchCondition(
        String keyword,
        String gender,
        Integer minAge,
        Integer maxAge,
        List<String> regions,
        List<String> universities,
        List<Long> subjectIds,
        String sort
) {
}
