package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomPreviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomPreviewLogRepository extends JpaRepository<ClassroomPreviewLog, Long> {
    int countByUserIdAndCourseId(Long userId, Long courseId);
}
