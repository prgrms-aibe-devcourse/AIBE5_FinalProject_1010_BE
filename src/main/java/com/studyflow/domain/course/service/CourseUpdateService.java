package com.studyflow.domain.course.service;

import com.studyflow.domain.subject.exception.SubjectNotFoundException;
import com.studyflow.domain.course.dto.create.CourseCreateResponse;
import com.studyflow.domain.course.dto.update.CourseUpdateRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.exception.CourseAccessForbiddenException;
import com.studyflow.domain.course.exception.CourseHasActiveStudentsException;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 수업 수정 / 삭제 서비스 — 선생님 본인 수업만 가능
@Service
@RequiredArgsConstructor
public class CourseUpdateService {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentRequestRepository enrollmentRequestRepository;

    // 수업 수정
    @Transactional
    public CourseCreateResponse updateCourse(Long courseId, Long teacherUserId, CourseUpdateRequest request) {
        // teacherProfile → user 까지 페치 (소유권 확인에 user.id 필요)
        Course course = courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        // 본인 수업인지 확인
        if (!course.getTeacherProfile().getUser().getId().equals(teacherUserId)) {
            throw new CourseAccessForbiddenException();
        }

        // 과목 변경 시 새 Subject 조회
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.getSubjectId()));

        // maxStudents 미입력 시 기존 값 유지
        int maxStudents = request.getMaxStudents() != null ? request.getMaxStudents() : course.getMaxStudents();

        // 현재 수강 중인 학생 수보다 정원을 작게 설정하는 것 방지
        long activeStudents = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        if (maxStudents < activeStudents) {
            throw new IllegalArgumentException(
                    "현재 수강 중인 학생(" + activeStudents + "명)보다 정원을 작게 설정할 수 없습니다.");
        }

        course.update(
                subject,
                request.getTitle(), request.getDescription(), request.getTargetGrade(),
                maxStudents, request.getDurationMinutes(), request.getPricePerSession(),
                request.getTextbook(), request.getCurriculumType(), request.getCurriculumDetail(),
                request.getAvailableSchedule(), request.getFirstClassDate(), request.getThumbnailUrl(),
                request.getRecruitDeadline(), request.getStartDate(), request.getEndDate(),
                request.getTeachingMode(), request.getLocation(),
                request.getLocationLat(), request.getLocationLng()
        );

        // @Transactional이라 별도 save 없이 변경사항 자동 반영 (dirty checking)
        return CourseCreateResponse.from(course);
    }

    // 수업 종료 — PATCH /api/v1/courses/{courseId}/close 전용 메서드
    // 소유권 검증, ACTIVE 수강생 확인, PENDING 일괄 거절, status=CLOSED 처리
    @Transactional
    public void closeCourse(Long courseId, Long teacherUserId) {
        Course course = courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        // 본인 수업인지 확인
        if (!course.getTeacherProfile().getUser().getId().equals(teacherUserId)) {
            throw new CourseAccessForbiddenException();
        }

        // 수강 중인 학생이 있으면 종료 불가
        long activeStudents = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        if (activeStudents > 0) {
            throw new CourseHasActiveStudentsException();
        }

        // 종료 전 PENDING 신청 일괄 거절 — 학생 신청 내역에 "대기 중"이 남지 않도록
        enrollmentRequestRepository.bulkRejectPendingByCourseId(courseId);

        // hard delete 대신 soft delete — Enrollment, ChatRoom 등 FK 참조로 인한 오류 방지
        course.close();
        courseRepository.save(course);
    }

    // 수업 삭제 (하위 호환 유지) — 실제로는 물리 삭제가 아닌 CLOSED 처리
    public void deleteCourse(Long courseId, Long teacherUserId) {
        closeCourse(courseId, teacherUserId);
    }
}
