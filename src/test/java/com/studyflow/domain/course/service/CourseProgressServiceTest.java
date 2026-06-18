package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.progress.CourseProgressCreateRequest;
import com.studyflow.domain.course.dto.progress.CourseProgressResponse;
import com.studyflow.domain.course.dto.progress.CourseProgressUpdateRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.entity.CourseProgress;
import com.studyflow.domain.course.exception.CourseAccessForbiddenException;
import com.studyflow.domain.course.exception.CourseProgressNotFoundException;
import com.studyflow.domain.course.repository.CourseProgressRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CourseProgressService 단위 테스트")
class CourseProgressServiceTest {

    @Mock CourseProgressRepository progressRepository;
    @Mock CourseAccessValidator accessValidator;

    CourseProgressService service;

    static final Long COURSE_ID = 1L;
    static final Long TEACHER_USER_ID = 10L;
    static final Long STUDENT_USER_ID = 20L;
    static final Long PROGRESS_ID = 100L;

    Course course;
    User teacherUser;

    @BeforeEach
    void setUp() {
        service = new CourseProgressService(progressRepository, accessValidator);

        teacherUser = mock(User.class);
        when(teacherUser.getId()).thenReturn(TEACHER_USER_ID);
        when(teacherUser.getName()).thenReturn("김선생");

        TeacherProfile teacherProfile = mock(TeacherProfile.class);
        when(teacherProfile.getUser()).thenReturn(teacherUser);

        course = mock(Course.class);
        when(course.getId()).thenReturn(COURSE_ID);
        when(course.getTeacherProfile()).thenReturn(teacherProfile);

        // 멤버 검증은 기본적으로 통과(course 반환)
        when(accessValidator.validateParticipantAndGetCourse(COURSE_ID, TEACHER_USER_ID)).thenReturn(course);
        when(accessValidator.validateParticipantAndGetCourse(COURSE_ID, STUDENT_USER_ID)).thenReturn(course);
        // save는 전달한 엔티티를 그대로 반환
        when(progressRepository.save(any(CourseProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        // 기본: 같은 날짜 진도 없음(신규 생성 경로)
        when(progressRepository.findByCourseIdAndProgressDateAndDeletedAtIsNull(eq(COURSE_ID), any()))
                .thenReturn(Optional.empty());
    }

    // ── 작성 ──────────────────────────────────────────────

    @Test
    @DisplayName("선생님이 진도를 작성하면 저장되고, 날짜 미지정 시 오늘 날짜로 저장된다")
    void createProgress_날짜미지정_오늘날짜() {
        CourseProgressCreateRequest req = newCreateRequest("1단원 함수 도입", null);

        CourseProgressResponse res = service.createProgress(COURSE_ID, TEACHER_USER_ID, req);

        assertThat(res.getContent()).isEqualTo("1단원 함수 도입");
        assertThat(res.getProgressDate()).isEqualTo(LocalDate.now());
        assertThat(res.getAuthorId()).isEqualTo(TEACHER_USER_ID);
        assertThat(res.getAuthorName()).isEqualTo("김선생");
    }

    @Test
    @DisplayName("작성 시 날짜를 지정하면 그 날짜로 저장된다")
    void createProgress_날짜지정() {
        CourseProgressCreateRequest req = newCreateRequest("2단원 극한", LocalDate.of(2026, 6, 10));

        CourseProgressResponse res = service.createProgress(COURSE_ID, TEACHER_USER_ID, req);

        assertThat(res.getProgressDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    @DisplayName("같은 수업·같은 날짜 진도가 이미 있으면 새로 만들지 않고 내용을 이어붙인다")
    void createProgress_같은날짜_이어붙임() {
        CourseProgress existing = CourseProgress.create(teacherUser, course, LocalDate.of(2026, 6, 18), "1교시 함수");
        when(progressRepository.findByCourseIdAndProgressDateAndDeletedAtIsNull(COURSE_ID, LocalDate.of(2026, 6, 18)))
                .thenReturn(Optional.of(existing));

        CourseProgressResponse res = service.createProgress(
                COURSE_ID, TEACHER_USER_ID, newCreateRequest("2교시 합성함수", LocalDate.of(2026, 6, 18)));

        assertThat(res.getContent()).isEqualTo("1교시 함수\n2교시 합성함수"); // 이어붙음
        verify(progressRepository, never()).save(any(CourseProgress.class)); // 신규 저장 안 함
    }

    @Test
    @DisplayName("담당 선생님이 아니면(검증 실패) 진도를 작성할 수 없다")
    void createProgress_선생님아님_예외() {
        doThrow(new CourseAccessForbiddenException())
                .when(accessValidator).validateTeacher(course, STUDENT_USER_ID);

        assertThatThrownBy(() -> service.createProgress(COURSE_ID, STUDENT_USER_ID, newCreateRequest("학생 작성", null)))
                .isInstanceOf(CourseAccessForbiddenException.class);
    }

    // ── 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("진도 목록을 응답 DTO로 매핑해 반환한다")
    void getProgressList_매핑() {
        CourseProgress p = CourseProgress.create(teacherUser, course, LocalDate.of(2026, 6, 18), "오늘 진도");
        Pageable pageable = PageRequest.of(0, 20);
        when(progressRepository.findByCourseIdAndDeletedAtIsNull(COURSE_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(p), pageable, 1));

        Page<CourseProgressResponse> page = service.getProgressList(COURSE_ID, STUDENT_USER_ID, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("오늘 진도");
    }

    @Test
    @DisplayName("존재하지 않는 진도 단건 조회 시 CourseProgressNotFoundException")
    void getProgress_없음_예외() {
        when(progressRepository.findByIdAndCourseIdAndDeletedAtIsNull(PROGRESS_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgress(COURSE_ID, PROGRESS_ID, STUDENT_USER_ID))
                .isInstanceOf(CourseProgressNotFoundException.class);
    }

    // ── 수정 ──────────────────────────────────────────────

    @Test
    @DisplayName("진도 수정 시 내용이 바뀌고, 날짜 미지정이면 기존 날짜를 유지한다")
    void updateProgress_날짜미지정_기존유지() {
        CourseProgress p = CourseProgress.create(teacherUser, course, LocalDate.of(2026, 6, 10), "수정 전");
        when(progressRepository.findByIdAndCourseIdAndDeletedAtIsNull(PROGRESS_ID, COURSE_ID))
                .thenReturn(Optional.of(p));

        CourseProgressResponse res = service.updateProgress(
                COURSE_ID, PROGRESS_ID, TEACHER_USER_ID, newUpdateRequest("수정 후", null));

        assertThat(res.getContent()).isEqualTo("수정 후");
        assertThat(res.getProgressDate()).isEqualTo(LocalDate.of(2026, 6, 10)); // 기존 유지
    }

    @Test
    @DisplayName("진도 수정 시 날짜를 주면 그 날짜로 바뀐다")
    void updateProgress_날짜지정() {
        CourseProgress p = CourseProgress.create(teacherUser, course, LocalDate.of(2026, 6, 10), "수정 전");
        when(progressRepository.findByIdAndCourseIdAndDeletedAtIsNull(PROGRESS_ID, COURSE_ID))
                .thenReturn(Optional.of(p));

        CourseProgressResponse res = service.updateProgress(
                COURSE_ID, PROGRESS_ID, TEACHER_USER_ID, newUpdateRequest("수정 후", LocalDate.of(2026, 6, 20)));

        assertThat(res.getProgressDate()).isEqualTo(LocalDate.of(2026, 6, 20));
    }

    // ── 삭제 ──────────────────────────────────────────────

    @Test
    @DisplayName("진도 삭제는 소프트 딜리트(deletedAt 설정)로 처리된다")
    void deleteProgress_소프트딜리트() {
        CourseProgress p = CourseProgress.create(teacherUser, course, LocalDate.now(), "삭제 대상");
        when(progressRepository.findByIdAndCourseIdAndDeletedAtIsNull(PROGRESS_ID, COURSE_ID))
                .thenReturn(Optional.of(p));

        service.deleteProgress(COURSE_ID, PROGRESS_ID, TEACHER_USER_ID);

        assertThat(p.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 진도 삭제 시 CourseProgressNotFoundException")
    void deleteProgress_없음_예외() {
        when(progressRepository.findByIdAndCourseIdAndDeletedAtIsNull(PROGRESS_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteProgress(COURSE_ID, PROGRESS_ID, TEACHER_USER_ID))
                .isInstanceOf(CourseProgressNotFoundException.class);
    }

    // ── 헬퍼 ──────────────────────────────────────────────

    // 요청 DTO는 @NoArgsConstructor + getter만 있어 테스트에서 리플렉션으로 필드 주입
    private CourseProgressCreateRequest newCreateRequest(String content, LocalDate date) {
        CourseProgressCreateRequest req = new CourseProgressCreateRequest();
        ReflectionTestUtils.setField(req, "content", content);
        ReflectionTestUtils.setField(req, "progressDate", date);
        return req;
    }

    private CourseProgressUpdateRequest newUpdateRequest(String content, LocalDate date) {
        CourseProgressUpdateRequest req = new CourseProgressUpdateRequest();
        ReflectionTestUtils.setField(req, "content", content);
        ReflectionTestUtils.setField(req, "progressDate", date);
        return req;
    }
}
