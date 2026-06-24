package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.CourseNoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseNoticeAttachmentRepository extends JpaRepository<CourseNoticeAttachment, Long> {

    // 공지에 속한 첨부파일을 표시 순서대로 조회
    List<CourseNoticeAttachment> findByCourseNoticeIdOrderBySortOrderAsc(Long courseNoticeId);

    // 공지 수정 시 기존 첨부파일을 전부 교체하기 위해 일괄 삭제
    void deleteByCourseNoticeId(Long courseNoticeId);
}
