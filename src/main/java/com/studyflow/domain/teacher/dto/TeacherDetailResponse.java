package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.enums.Gender;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 선생님 상세 페이지(/teachers/:id) 응답 DTO
@Getter
@Builder
public class TeacherDetailResponse {

    private Long id;                    // TeacherProfile.id
    private String name;                // User.name
    private String profileImageUrl;     // User.profileImageUrl
    private Gender gender;              // User.gender
    private String education;           // 학력 (전공 포함)
    private String career;              // 경력
    private String awards;              // 수상내역
    private Integer age;
    private String address;             // 활동 지역
    private String teachingStyle;       // 수업 방식
    private String introduction;        // 자기소개 본문
    private int naegongScore;           // 내공 점수

    // 운영 중인 수업 카드 목록 (isListed=true + RECRUITING or IN_PROGRESS)
    private List<TeacherCourseCardResponse> courses;

    // TeacherProfile + 수업 목록을 조합해 상세 응답 생성
    // user는 JOIN FETCH 후 전달해야 LazyInitializationException 방지
    public static TeacherDetailResponse of(TeacherProfile profile, List<TeacherCourseCardResponse> courses) {
        return TeacherDetailResponse.builder()
                .id(profile.getId())
                .name(profile.getUser().getName())
                .profileImageUrl(profile.getUser().getProfileImageUrl())
                .gender(profile.getUser().getGender())
                .education(profile.getEducation())
                .career(profile.getCareer())
                .awards(profile.getAwards())
                .age(profile.getAge())
                .address(profile.getAddress())
                .teachingStyle(profile.getTeachingStyle())
                .introduction(profile.getIntroduction())
                .naegongScore(profile.getNaegongScore())
                .courses(courses)
                .build();
    }
}
