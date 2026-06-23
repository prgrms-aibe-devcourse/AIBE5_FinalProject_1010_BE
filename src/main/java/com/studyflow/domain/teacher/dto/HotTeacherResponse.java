package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import lombok.Builder;
import lombok.Getter;

// 메인 홈 "이번주 HOT 선생님" 카드 응답 DTO — 지난 7일 내공 획득량 상위 선생님
@Getter
@Builder
public class HotTeacherResponse {

    private int rank;                  // 1부터 시작하는 순위
    private Long teacherProfileId;     // TeacherProfile.id (상세 페이지 이동용)
    private String name;               // User.name
    private String profileImageUrl;    // User.profileImageUrl (nullable)
    private String subject;            // 대표 전문 과목명 (없으면 null)
    private long weeklyNaegongGain;    // 지난 7일 내공 획득 합계 (fallback 선생님은 0)

    // user는 JOIN FETCH 후 전달해야 LazyInitializationException 방지
    public static HotTeacherResponse of(TeacherProfile profile, int rank, String subject, long weeklyNaegongGain) {
        return HotTeacherResponse.builder()
                .rank(rank)
                .teacherProfileId(profile.getId())
                .name(profile.getUser().getName())
                .profileImageUrl(profile.getUser().getProfileImageUrl())
                .subject(subject)
                .weeklyNaegongGain(weeklyNaegongGain)
                .build();
    }
}
