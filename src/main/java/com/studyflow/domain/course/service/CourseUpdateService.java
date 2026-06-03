package com.studyflow.domain.course.service;

import com.studyflow.domain.ai.exception.SubjectNotFoundException;
import com.studyflow.domain.course.dto.create.CourseCreateResponse;
import com.studyflow.domain.course.dto.update.CourseUpdateRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.exception.CourseAccessForbiddenException;
import com.studyflow.domain.course.exception.CourseHasActiveStudentsException;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
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

        course.update(
                subject,
                request.getTitle(), request.getDescription(), request.getTargetGrade(),
                maxStudents, request.getDurationMinutes(), request.getPricePerSession(),
                request.getTextbook(), request.getCurriculumType(), request.getCurriculumDetail(),
                request.getAvailableSchedule(), request.getFirstClassDate(), request.getThumbnailUrl(),
                request.getRecruitDeadline(), request.getStartDate(), request.getEndDate()
        );

        // @Transactional이라 별도 save 없이 변경사항 자동 반영 (dirty checking)
        return CourseCreateResponse.from(course);
    }

    // 수업 삭제
    @Transactional
    public void deleteCourse(Long courseId, Long teacherUserId) {
        Course course = courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        // 본인 수업인지 확인
        if (!course.getTeacherProfile().getUser().getId().equals(teacherUserId)) {
            throw new CourseAccessForbiddenException();
        }

        // 수강 중인 학생이 있으면 삭제 불가
        long activeStudents = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        if (activeStudents > 0) {
            throw new CourseHasActiveStudentsException();
        }

        courseRepository.delete(course);
    }
}
