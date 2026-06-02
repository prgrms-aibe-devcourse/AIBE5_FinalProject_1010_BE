package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import lombok.Builder;
import lombok.Getter;

// 메인 페이지 선생님 카드 슬라이드에 사용하는 응답 DTO
@Getter
@Builder
public class TeacherCardResponse {

    private Long id;                    // TeacherProfile.id
    private String name;                // User.name
    private String profileImageUrl;     // User.profileImageUrl
    private String education;           // 학력 (예: "서울대 수학과 재학")
    private String career;              // 경력 요약
    private int naegongScore;           // 내공 점수
    private long courseCount;           // 현재 공개 중인 수업 수

    // TeacherProfile + 수업 수를 조합해 카드 응답 생성
    // user는 JOIN FETCH 후 전달해야 LazyInitializationException 방지
    public static TeacherCardResponse of(TeacherProfile profile, long courseCount) {
        return TeacherCardResponse.builder()
                .id(profile.getId())
                .name(profile.getUser().getName())
                .profileImageUrl(profile.getUser().getProfileImageUrl())
                .education(profile.getEducation())
                .career(profile.getCareer())
                .naegongScore(profile.getNaegongScore())
                .courseCount(courseCount)
                .build();
    }
}
