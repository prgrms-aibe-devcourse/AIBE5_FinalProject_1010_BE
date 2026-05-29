package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.CoursePostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePostAttachmentRepository extends JpaRepository<CoursePostAttachment, Long> {

    // 게시글에 속한 첨부파일을 표시 순서대로 조회
    List<CoursePostAttachment> findByCoursePostIdOrderBySortOrderAsc(Long coursePostId);

    // 게시글 수정 시 기존 첨부파일을 전부 교체하기 위해 일괄 삭제
    void deleteByCoursePostId(Long coursePostId);
}
