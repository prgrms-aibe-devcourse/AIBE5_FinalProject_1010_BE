package com.studyflow.domain.course.dto.search;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.TargetGrade;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Builder
public class CourseCardResponse {

    private Long id;
    private String title;

    // 선생님 이름 (TeacherProfile → User)
    private String teacherName;

    // 선생님 프로필 사진 URL (TeacherProfile → User)
    private String teacherProfileImageUrl;

    // 과목명 (Subject)
    private String subjectName;

    private TargetGrade targetGrade;

    // 회당 수업료 (원)
    private int pricePerSession;

    // 최대 정원
    private int maxStudents;

    // 현재 수강 중인 학생 수 (Enrollment ACTIVE 기준)
    private long currentStudents;

    // 리뷰 평균 평점 (Review 테이블 AVG, 소수점 1자리 반올림)
    private BigDecimal avgRating;

    // 리뷰 건수
    private long reviewCount;

    private String thumbnailUrl;
    private CourseStatus status;

    // course 엔티티 + 별도 집계값을 조합해 카드 응답 생성
    // teacherProfile, user, subject는 JOIN FETCH 후 전달해야 LazyInitializationException
    // 방지
    public static CourseCardResponse of(Course course, long currentStudents, double avgRating, long reviewCount) {
        return CourseCardResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .teacherName(course.getTeacherProfile().getUser().getName())
                .teacherProfileImageUrl(course.getTeacherProfile().getUser().getProfileImageUrl())
                .subjectName(course.getSubject().getName())
                .targetGrade(course.getTargetGrade())
                .pricePerSession(course.getPricePerSession())
                .maxStudents(course.getMaxStudents())
                .currentStudents(currentStudents)
                .avgRating(BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP))
                .reviewCount(reviewCount)
                .thumbnailUrl(course.getThumbnailUrl())
                .status(course.getStatus())
                .build();
    }
}
