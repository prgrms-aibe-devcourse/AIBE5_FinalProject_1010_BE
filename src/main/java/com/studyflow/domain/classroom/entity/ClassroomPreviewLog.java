package com.studyflow.domain.classroom.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 강의실 미리보기 이력 — 로그인 유저가 특정 수업을 미리본 기록.
 * 수업(courseId)당 동일 유저(userId)의 미리보기를 최대 2회로 제한하는 데 사용된다.
 */
@Entity
@Table(
        name = "classroom_preview_log",
        indexes = @Index(name = "idx_preview_log_user_course", columnList = "user_id, course_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassroomPreviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static ClassroomPreviewLog of(Long userId, Long courseId) {
        ClassroomPreviewLog log = new ClassroomPreviewLog();
        log.userId = userId;
        log.courseId = courseId;
        return log;
    }
}
