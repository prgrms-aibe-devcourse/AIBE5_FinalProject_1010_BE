package com.studyflow.domain.course.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수업 진도 엔티티 (이슈 #173)
 *
 * 선생님이 수업별로 "어느 날, 어디까지 나갔는지"를 짧게 기록한다.
 * 날짜(progressDate) + 짤막한 내용(content)만 갖는 단순 구조. 소프트 딜리트(deletedAt).
 */
@Entity
@Table(
        name = "course_progress",
        indexes = {
                @Index(name = "idx_course_progress_course", columnList = "course_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 진도를 작성한 선생님
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 진도가 속한 수업
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // 진도 날짜 (예: 2026-06-18 수업에서 나간 진도)
    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;

    // 짤막한 진도 내용 ("3단원 함수 ~ 4단원 미분 도입" 등)
    @Column(length = 1000, nullable = false)
    private String content;

    // null이면 정상, 값이 있으면 소프트 딜리트
    @Column
    private LocalDateTime deletedAt;

    public static CourseProgress create(User user, Course course, LocalDate progressDate, String content) {
        CourseProgress progress = new CourseProgress();
        progress.user = user;
        progress.course = course;
        progress.progressDate = progressDate;
        progress.content = content;
        return progress;
    }

    public void update(LocalDate progressDate, String content) {
        this.progressDate = progressDate;
        this.content = content;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
