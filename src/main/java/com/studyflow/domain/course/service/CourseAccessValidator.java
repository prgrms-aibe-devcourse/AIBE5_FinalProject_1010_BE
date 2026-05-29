package com.studyflow.domain.course.service;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.exception.CourseAccessForbiddenException;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.exception.NotCourseParticipantException;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 수업별 페이지 전반에서 공통으로 사용하는 접근 권한 검증 컴포넌트
@Component
@RequiredArgsConstructor
public class CourseAccessValidator {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    // 선생님 또는 활성 수강생인지 확인 후 Course 반환 (teacherProfile·user·subject JOIN FETCH 상태)
    public Course validateParticipantAndGetCourse(Long courseId, Long userId) {
        Course course = courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        boolean isTeacher = course.getTeacherProfile().getUser().getId().equals(userId);
        boolean isActiveStudent = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId, courseId, EnrollmentStatus.ACTIVE);

        if (!isTeacher && !isActiveStudent) {
            throw new NotCourseParticipantException();
        }
        return course;
    }

    // 담당 선생님이 아니면 403
    public void validateTeacher(Course course, Long userId) {
        if (!course.getTeacherProfile().getUser().getId().equals(userId)) {
            throw new CourseAccessForbiddenException();
        }
    }

    // 담당 선생님 여부만 반환 — 삭제처럼 역할에 따라 분기할 때 사용
    public boolean isTeacher(Course course, Long userId) {
        return course.getTeacherProfile().getUser().getId().equals(userId);
    }
}
