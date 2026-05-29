package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.dashboard.CourseDashboardResponse;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.enrollment.dto.EnrolledStudentResponse;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseDashboardService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseAccessValidator accessValidator;

    // 수업별 페이지 상단 대시보드 정보 조회
    public CourseDashboardResponse getDashboard(Long courseId, Long userId) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        long enrolledCount = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        return CourseDashboardResponse.of(course, enrolledCount);
    }

    // 수강생 목록 — ACTIVE 상태만 반환 (탈퇴·강제퇴장 학생 제외)
    public List<EnrolledStudentResponse> getEnrolledStudents(Long courseId, Long userId) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        return enrollmentRepository.findWithUserByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(EnrolledStudentResponse::from)
                .toList();
    }
}
