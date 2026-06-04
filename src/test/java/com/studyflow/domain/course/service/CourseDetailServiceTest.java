package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.detail.CourseDetailResponse;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseDetailService 단위 테스트")
class CourseDetailServiceTest {

    // ── 의존성 Mock ──────────────────────────────────────────
    @Mock CourseRepository courseRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock EnrollmentRequestRepository enrollmentRequestRepository;

    CourseDetailService service;

    // ── 공통 픽스처 ──────────────────────────────────────────
    // 테스트마다 반복되는 mock 객체 생성을 줄이기 위해 공통으로 정의
    static final Long COURSE_ID = 1L;
    static final Long TEACHER_USER_ID = 10L;
    static final Long STUDENT_USER_ID = 20L;
    static final Long OTHER_TEACHER_USER_ID = 30L;

    Course course;

    @BeforeEach
    void setUp() {
        service = new CourseDetailService(courseRepository, enrollmentRepository, enrollmentRequestRepository);

        // 수업, 선생님, 과목 mock 구성
        User teacherUser = mockUser(TEACHER_USER_ID);
        TeacherProfile teacherProfile = mockTeacherProfile(teacherUser);
        Subject subject = mockSubject();
        course = mockCourse(teacherProfile, subject);

        // findWithTeacherAndSubjectById는 항상 위 course를 반환
        when(courseRepository.findWithTeacherAndSubjectById(COURSE_ID)).thenReturn(Optional.of(course));
        // 수강생 수 집계 기본값
        when(enrollmentRepository.countByCourseIdAndStatus(COURSE_ID, EnrollmentStatus.ACTIVE)).thenReturn(2L);
        when(enrollmentRepository.countByTeacherProfileId(teacherProfile.getId())).thenReturn(50L);
    }

    // ── getCourseDetail — 비로그인 ────────────────────────────

    @Test
    @DisplayName("비로그인(userId=null)이면 myStatus는 null")
    void getCourseDetail_비로그인_myStatus_null() {
        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, null, null);

        assertThat(response.getMyStatus()).isNull();
    }

    // ── getCourseDetail — myStatus 계산 ──────────────────────

    @Test
    @DisplayName("이 수업의 선생님 본인이면 myStatus=OWNER")
    void getCourseDetail_myStatus_OWNER() {
        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, TEACHER_USER_ID, "TEACHER");

        assertThat(response.getMyStatus()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("다른 선생님(TEACHER 역할)이면 myStatus=TEACHER")
    void getCourseDetail_myStatus_TEACHER() {
        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, OTHER_TEACHER_USER_ID, "TEACHER");

        assertThat(response.getMyStatus()).isEqualTo("TEACHER");
    }

    @Test
    @DisplayName("ACTIVE 수강 중이면 myStatus=ENROLLED")
    void getCourseDetail_myStatus_ENROLLED() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                STUDENT_USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).thenReturn(true);

        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, STUDENT_USER_ID, "STUDENT");

        assertThat(response.getMyStatus()).isEqualTo("ENROLLED");
    }

    @Test
    @DisplayName("수강 신청 PENDING이면 myStatus=PENDING")
    void getCourseDetail_myStatus_PENDING() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                STUDENT_USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);
        when(enrollmentRequestRepository.findFirstByUserIdAndCourseIdOrderByCreatedAtDesc(
                STUDENT_USER_ID, COURSE_ID)).thenReturn(Optional.of(mockEnrollmentRequest(EnrollmentRequestStatus.PENDING)));

        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, STUDENT_USER_ID, "STUDENT");

        assertThat(response.getMyStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("수강 신청 REJECTED이면 myStatus=REJECTED")
    void getCourseDetail_myStatus_REJECTED() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                STUDENT_USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);
        when(enrollmentRequestRepository.findFirstByUserIdAndCourseIdOrderByCreatedAtDesc(
                STUDENT_USER_ID, COURSE_ID)).thenReturn(Optional.of(mockEnrollmentRequest(EnrollmentRequestStatus.REJECTED)));

        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, STUDENT_USER_ID, "STUDENT");

        assertThat(response.getMyStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("수강 신청 CANCELLED이면 myStatus=CANCELLED")
    void getCourseDetail_myStatus_CANCELLED() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                STUDENT_USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);
        when(enrollmentRequestRepository.findFirstByUserIdAndCourseIdOrderByCreatedAtDesc(
                STUDENT_USER_ID, COURSE_ID)).thenReturn(Optional.of(mockEnrollmentRequest(EnrollmentRequestStatus.CANCELLED)));

        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, STUDENT_USER_ID, "STUDENT");

        assertThat(response.getMyStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("신청 이력이 없으면 myStatus=NOT_APPLIED")
    void getCourseDetail_myStatus_NOT_APPLIED() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                STUDENT_USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);
        when(enrollmentRequestRepository.findFirstByUserIdAndCourseIdOrderByCreatedAtDesc(
                STUDENT_USER_ID, COURSE_ID)).thenReturn(Optional.empty());

        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, STUDENT_USER_ID, "STUDENT");

        assertThat(response.getMyStatus()).isEqualTo("NOT_APPLIED");
    }

    // ── getCourseDetail — 예외 ────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 수업 ID이면 CourseNotFoundException")
    void getCourseDetail_수업없음_예외() {
        when(courseRepository.findWithTeacherAndSubjectById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCourseDetail(999L, STUDENT_USER_ID, "STUDENT"))
                .isInstanceOf(CourseNotFoundException.class);
    }

    // ── 응답 데이터 검증 ─────────────────────────────────────

    @Test
    @DisplayName("응답에 현재 수강생 수와 선생님 누적 수강생 수가 포함된다")
    void getCourseDetail_수강생수_정상포함() {
        CourseDetailResponse response = service.getCourseDetail(COURSE_ID, null, null);

        assertThat(response.getCurrentStudents()).isEqualTo(2L);
        assertThat(response.getTeacher().getTotalEnrolledStudents()).isEqualTo(50L);
    }

    // ── Mock 생성 헬퍼 ──────────────────────────────────────

    private User mockUser(Long userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        return user;
    }

    private TeacherProfile mockTeacherProfile(User user) {
        TeacherProfile profile = mock(TeacherProfile.class);
        when(profile.getId()).thenReturn(100L);
        when(profile.getUser()).thenReturn(user);
        when(profile.getNaegongScore()).thenReturn(0);
        when(profile.getTotalTeachingHours()).thenReturn(BigDecimal.ZERO);
        return profile;
    }

    private Subject mockSubject() {
        Subject subject = mock(Subject.class);
        when(subject.getName()).thenReturn("수학");
        return subject;
    }

    private Course mockCourse(TeacherProfile teacherProfile, Subject subject) {
        Course c = mock(Course.class);
        when(c.getId()).thenReturn(COURSE_ID);
        when(c.getTeacherProfile()).thenReturn(teacherProfile);
        when(c.getSubject()).thenReturn(subject);
        return c;
    }

    private EnrollmentRequest mockEnrollmentRequest(EnrollmentRequestStatus status) {
        EnrollmentRequest req = mock(EnrollmentRequest.class);
        when(req.getStatus()).thenReturn(status);
        return req;
    }
}
