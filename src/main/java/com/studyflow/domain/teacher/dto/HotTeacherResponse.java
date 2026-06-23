package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import lombok.Builder;

// 메인 홈 "이번주 HOT 선생님" 카드 응답 DTO — 지난 7일 내공 획득량 상위 선생님
// record로 둬야 Redis 캐시(GenericJackson2JsonRedisSerializer)가 canonical 생성자로 역직렬화할 수 있다.
// (Lombok @Getter @Builder 클래스는 no-arg 생성자·setter가 없어 캐시 복원 시 500이 났음)
@Builder
public record HotTeacherResponse(
        int rank,                  // 1부터 시작하는 순위
        Long teacherProfileId,     // TeacherProfile.id (상세 페이지 이동용)
        String name,               // User.name
        String profileImageUrl,    // User.profileImageUrl (nullable)
        String subject,            // 대표 전문 과목명 (없으면 null)
        long weeklyNaegongGain     // 지난 7일 내공 획득 합계 (fallback 선생님은 0)
) {

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
