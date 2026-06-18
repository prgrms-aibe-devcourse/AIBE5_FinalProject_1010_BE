package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.create.CourseCreateRequest;
import com.studyflow.domain.course.dto.create.CourseCreateResponse;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.subject.exception.SubjectNotFoundException;
import com.studyflow.domain.teacher.exception.TeacherProfileNotFoundException;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 수업 등록 서비스 — 선생님 전용
@Service
@RequiredArgsConstructor
public class CourseCreateService {

    private final CourseRepository courseRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    public CourseCreateResponse createCourse(Long teacherUserId, CourseCreateRequest request) {
        // 로그인한 선생님의 프로필 조회 — 수업에 teacher_profile_id 연결 필요
        TeacherProfile teacherProfile = teacherProfileRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(teacherUserId));

        // 요청한 과목 조회
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.getSubjectId()));

        // maxStudents 미입력 시 기본값 1 (MVP는 1:1 수업 기준)
        int maxStudents = request.getMaxStudents() != null ? request.getMaxStudents() : 1;

        // 수업 엔티티 생성 — 기본 status=RECRUITING, isListed=true로 설정됨
        Course course = Course.create(
                teacherProfile, subject,
                request.getTitle(), request.getDescription(), request.getTargetGrade(),
                maxStudents, request.getDurationMinutes(), request.getPricePerSession(),
                request.getTextbook(), request.getCurriculumType(), request.getCurriculumDetail(),
                request.getAvailableSchedule(), request.getFirstClassDate(), request.getThumbnailUrl(),
                request.getRecruitDeadline(), request.getStartDate(), request.getEndDate(),
                request.getTeachingMode(), request.getLocation(),
                request.getLocationLat(), request.getLocationLng()
        );

        Course saved = courseRepository.save(course);

        // 첫 수업 등록 시 선생님 찾기 목록에 자동 노출 — 이미 노출 중이면 변화 없음
        // @Transactional dirty checking으로 자동 반영되므로 별도 save 불필요
        if (!teacherProfile.isListed()) {
            teacherProfile.updateListed(true);
        }

        return CourseCreateResponse.from(saved);
    }
}
