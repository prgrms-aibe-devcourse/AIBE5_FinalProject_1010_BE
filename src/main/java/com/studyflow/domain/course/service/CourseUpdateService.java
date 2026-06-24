package com.studyflow.domain.course.service;

import com.studyflow.domain.assignment.repository.AssignmentRepository;
import com.studyflow.domain.chat.repository.ChatRoomRepository;
import com.studyflow.domain.classroom.repository.ClassroomSessionRepository;
import com.studyflow.domain.course.dto.create.CourseCreateResponse;
import com.studyflow.domain.course.dto.update.CourseUpdateRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.exception.CourseAccessForbiddenException;
import com.studyflow.domain.course.exception.CourseAlreadyClosedException;
import com.studyflow.domain.course.exception.CourseHasActiveStudentsException;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.exception.CourseNotDeletableException;
import org.springframework.dao.DataIntegrityViolationException;
import com.studyflow.domain.course.repository.CourseNoticeRepository;
import com.studyflow.domain.course.repository.CoursePostRepository;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.exception.SubjectNotFoundException;
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
    private final CoursePostRepository coursePostRepository;
    private final CourseNoticeRepository courseNoticeRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClassroomSessionRepository classroomSessionRepository;
    private final ChatRoomRepository chatRoomRepository;

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

        // 수강 중인 학생이 1명이라도 있으면 수정 불가
        long activeStudents = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        if (activeStudents > 0) {
            throw new CourseHasActiveStudentsException();
        }

        // 과목 변경 시 새 Subject 조회
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.getSubjectId()));

        // maxStudents 미입력 시 기존 값 유지
        int maxStudents = request.getMaxStudents() != null ? request.getMaxStudents() : course.getMaxStudents();
        // ACTIVE 수강생이 있으면 수업 수정 자체를 차단하므로, 기존 정원 축소 검증은 별도로 수행하지 않는다.

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

        assertTeacherOwner(course, teacherUserId);

        // 이미 종료된 수업 재종료 방지 — 내공 중복 지급 차단
        if (course.getStatus() == CourseStatus.CLOSED) {
            throw new CourseAlreadyClosedException();
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

    // 수업 삭제 — RECRUITING 상태이며 관련 데이터가 전혀 없는 수업만 실제 삭제 허용
    // 수강 신청·게시글·공지·과제·강의실·채팅방 중 하나라도 존재하면 삭제 불가
    // 이미 사용된 수업은 PATCH /close 종료 API를 사용해야 함
    @Transactional
    public void deleteCourse(Long courseId, Long teacherUserId) {
        Course course = courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        assertTeacherOwner(course, teacherUserId);

        // RECRUITING 상태만 삭제 허용
        if (course.getStatus() != CourseStatus.RECRUITING) {
            throw new CourseNotDeletableException();
        }

        // 관련 데이터가 하나라도 존재하면 삭제 불가
        if (enrollmentRepository.existsByCourseId(courseId)
                || enrollmentRequestRepository.existsByCourseId(courseId)
                || coursePostRepository.existsByCourseId(courseId)
                || courseNoticeRepository.existsByCourseId(courseId)
                || assignmentRepository.existsByCourseId(courseId)
                || classroomSessionRepository.existsByCourseId(courseId)
                || chatRoomRepository.existsByCourseId(courseId)) {
            throw new CourseNotDeletableException();
        }

        try {
            courseRepository.delete(course);
        } catch (DataIntegrityViolationException e) {
            throw new CourseNotDeletableException();
        }
    }

    private void assertTeacherOwner(Course course, Long teacherUserId) {
        if (!course.getTeacherProfile().getUser().getId().equals(teacherUserId)) {
            throw new CourseAccessForbiddenException();
        }
    }
}
