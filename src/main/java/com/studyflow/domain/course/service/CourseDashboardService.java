package com.studyflow.domain.course.service;

import com.studyflow.domain.classroom.enums.ClassroomStatus;
import com.studyflow.domain.classroom.repository.ClassroomParticipantRepository;
import com.studyflow.domain.classroom.repository.ClassroomSessionRepository;
import com.studyflow.domain.course.dto.dashboard.AttendanceResponse;
import com.studyflow.domain.course.dto.dashboard.CourseDashboardResponse;
import com.studyflow.domain.course.dto.dashboard.NextClassRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.enrollment.dto.EnrolledStudentResponse;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseDashboardService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseAccessValidator accessValidator;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomParticipantRepository participantRepository;

    // 수업별 페이지 상단 대시보드 정보 조회
    public CourseDashboardResponse getDashboard(Long courseId, Long userId) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        long enrolledCount = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        return CourseDashboardResponse.of(course, enrolledCount);
    }

    // 다음 수업 일시 설정 — 선생님만 가능
    @Transactional
    public CourseDashboardResponse updateNextClass(Long courseId, Long userId, NextClassRequest req) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);
        course.updateNextClassAt(req.getNextClassAt());
        long enrolledCount = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        return CourseDashboardResponse.of(course, enrolledCount);
    }

    // 출석 현황 — CLOSED 세션 기준으로 수강생별 입장 횟수 집계
    public List<AttendanceResponse> getAttendance(Long courseId, Long userId) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);

        List<Long> sessionIds = sessionRepository.findSessionIdsByCourseIdAndStatus(courseId, ClassroomStatus.CLOSED);
        long totalSessions = sessionIds.size();

        Map<Long, Long> countMap = sessionIds.isEmpty()
                ? Map.of()
                : participantRepository.countBySessionIds(sessionIds).stream()
                        .collect(Collectors.toMap(
                                ClassroomParticipantRepository.UserAttendanceCount::getUserId,
                                ClassroomParticipantRepository.UserAttendanceCount::getCount));

        return enrollmentRepository.findWithUserByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(e -> AttendanceResponse.builder()
                        .userId(e.getUser().getId())
                        .name(e.getUser().getName())
                        .profileImageUrl(e.getUser().getProfileImageUrl())
                        .attendedCount(countMap.getOrDefault(e.getUser().getId(), 0L))
                        .totalSessions(totalSessions)
                        .build())
                .toList();
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
