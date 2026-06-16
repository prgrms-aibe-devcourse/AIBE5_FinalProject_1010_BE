package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.enums.Gender;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// 선생님 상세 페이지(/teachers/:id) 응답 DTO
@Getter
@Builder
public class TeacherDetailResponse {

    private Long id;                    // TeacherProfile.id
    private Long userId;               // User.id (채팅방 생성에 필요)
    private String name;                // User.name
    private String profileImageUrl;     // User.profileImageUrl
    private Gender gender;              // User.gender
    private String career;              // 대학교
    private String major;               // 전공
    private String admissionYear;       // 학번
    private String awards;              // 수상내역
    private String address;             // 활동 지역
    private String teachingStyle;       // 수업 방식
    private String introduction;        // 자기소개 본문
    private int naegongScore;           // 내공 점수
    private BigDecimal totalTeachingHours;       // 누적 수업 시간
    private List<String> specialtySubjects;      // 전문 과목명 목록

    // 질문게시판 활동 통계
    private long answerCount;           // 작성한 답변 수
    private Integer acceptRate;         // 채택률(%) — 답변이 없으면 null

    // 운영 중인 수업 카드 목록 (isListed=true + RECRUITING or IN_PROGRESS)
    private List<TeacherCourseCardResponse> courses;

    // TeacherProfile + 수업 목록 + QnA 활동 통계를 조합해 상세 응답 생성
    // user/specialtySubjects는 트랜잭션 내에서 접근해야 LazyInitializationException 방지
    public static TeacherDetailResponse of(TeacherProfile profile,
                                           List<TeacherCourseCardResponse> courses,
                                           long answerCount, long acceptedCount) {
        Integer acceptRate = answerCount > 0
                ? (int) Math.min(100, Math.round(acceptedCount * 100.0 / answerCount))
                : null;

        List<String> specialtySubjects = profile.getSpecialtySubjects().stream()
                .map(Subject::getName)
                .toList();

        return TeacherDetailResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .profileImageUrl(profile.getUser().getProfileImageUrl())
                .gender(profile.getUser().getGender())
                .career(profile.getCareer())
                .major(profile.getMajor())
                .admissionYear(profile.getAdmissionYear())
                .awards(profile.getAwards())
                .address(profile.getAddress())
                .teachingStyle(profile.getTeachingStyle())
                .introduction(profile.getIntroduction())
                .naegongScore(profile.getNaegongScore())
                .totalTeachingHours(profile.getTotalTeachingHours())
                .specialtySubjects(specialtySubjects)
                .answerCount(answerCount)
                .acceptRate(acceptRate)
                .courses(courses)
                .build();
    }
}
