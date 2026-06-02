package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.detail.CourseDetailResponse;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 수업 상세 페이지 — 수업 정보 + 선생님 요약 + 로그인 사용자의 수강 상태(myStatus) 조합
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseDetailService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentRequestRepository enrollmentRequestRepository;

    // currentUserId가 null이면 비로그인 — myStatus는 null로 반환
    public CourseDetailResponse getCourseDetail(Long courseId, Long currentUserId) {
        // teacherProfile → user, subject 한 번에 페치 (N+1 방지)
        Course course = courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        // 현재 이 수업을 수강 중인 학생 수
        long currentStudents = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);

        // 선생님의 전체 수업 누적 수강생 수 (신뢰도 지표)
        long totalEnrolledStudents = enrollmentRepository
                .countByTeacherProfileIdAndStatus(course.getTeacherProfile().getId(), EnrollmentStatus.ACTIVE);

        String myStatus = currentUserId == null ? null : determineMyStatus(currentUserId, course);

        return CourseDetailResponse.of(course, currentStudents, totalEnrolledStudents, myStatus);
    }

    // myStatus 판단 순서: 선생님 본인 → 수강 중 → 최근 신청 상태 → 미신청
    private String determineMyStatus(Long userId, Course course) {
        if (course.getTeacherProfile().getUser().getId().equals(userId)) {
            return "OWNER";
        }

        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, course.getId(), EnrollmentStatus.ACTIVE)) {
            return "ENROLLED";
        }

        // 신청/취소를 반복했을 수 있으므로 가장 최근 신청 기준으로 판단
        return enrollmentRequestRepository
                .findFirstByUserIdAndCourseIdOrderByCreatedAtDesc(userId, course.getId())
                .map(req -> req.getStatus().name())
                .orElse("NOT_APPLIED");
    }
}
