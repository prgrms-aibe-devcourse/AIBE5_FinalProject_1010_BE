package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.TargetGrade;
import lombok.Builder;
import lombok.Getter;

// 선생님 상세 페이지의 운영 중인 수업 카드에 사용하는 응답 DTO
// CourseCardResponse와 달리 수강생 수·선생님 정보는 불필요하므로 별도 경량 DTO 사용
@Getter
@Builder
public class TeacherCourseCardResponse {

    private Long id;
    private String title;
    private String subjectName;     // Subject.name
    private TargetGrade targetGrade;
    private int pricePerSession;
    private int maxStudents;
    private int durationMinutes;    // 회당 수업 시간 (분)
    private String thumbnailUrl;
    private CourseStatus status;

    // subject는 JOIN FETCH 후 전달해야 LazyInitializationException 방지
    public static TeacherCourseCardResponse from(Course course) {
        return TeacherCourseCardResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .subjectName(course.getSubject().getName())
                .targetGrade(course.getTargetGrade())
                .pricePerSession(course.getPricePerSession())
                .maxStudents(course.getMaxStudents())
                .durationMinutes(course.getDurationMinutes())
                .thumbnailUrl(course.getThumbnailUrl())
                .status(course.getStatus())
                .build();
    }
}
