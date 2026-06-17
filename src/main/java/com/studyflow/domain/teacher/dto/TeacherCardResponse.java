package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// 선생님 찾기/메인 페이지 선생님 카드에 사용하는 응답 DTO
@Getter
@Builder
public class TeacherCardResponse {

    private Long id;                    // TeacherProfile.id
    private String name;                // User.name
    private String profileImageUrl;     // User.profileImageUrl
    private String career;              // 대학교
    private String major;               // 전공
    private String admissionYear;       // 학번
    private int naegongScore;           // 내공 점수
    private long courseCount;           // 현재 공개 중인 수업 수
    private String address;             // 활동 지역
    private BigDecimal totalTeachingHours; // 누적 수업 시간
    private List<String> specialtySubjects; // 전문 과목명 목록
    private boolean verified;           // 관리자 인증 완료 여부 (User.isVerified)

    // TeacherProfile + 수업 수 + 전문 과목명을 조합해 카드 응답 생성
    // user는 JOIN FETCH 후 전달해야 LazyInitializationException 방지
    // specialtySubjects는 N+1 방지를 위해 서비스에서 일괄 조회해 전달 (지연 로딩 직접 접근 금지)
    public static TeacherCardResponse of(TeacherProfile profile, long courseCount, List<String> specialtySubjects) {
        return TeacherCardResponse.builder()
                .id(profile.getId())
                .name(profile.getUser().getName())
                .profileImageUrl(profile.getUser().getProfileImageUrl())
                .career(profile.getCareer())
                .major(profile.getMajor())
                .admissionYear(profile.getAdmissionYear())
                .naegongScore(profile.getNaegongScore())
                .courseCount(courseCount)
                .address(profile.getAddress())
                .totalTeachingHours(profile.getTotalTeachingHours())
                .specialtySubjects(specialtySubjects)
                .verified(profile.getUser().isVerified())
                .build();
    }
}
