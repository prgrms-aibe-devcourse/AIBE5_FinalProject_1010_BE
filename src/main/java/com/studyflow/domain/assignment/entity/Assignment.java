package com.studyflow.domain.assignment.entity;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime dueDate;

    public static Assignment create(Course course, String title, String content, LocalDateTime dueDate) {
        Assignment a = new Assignment();
        a.course = course;
        a.title = title;
        a.content = content;
        a.dueDate = dueDate;
        return a;
    }

    public void update(String title, String content, LocalDateTime dueDate) {
        this.title = title;
        this.content = content;
        this.dueDate = dueDate;
    }
}
