package com.studyflow.domain.teacher.service;

import com.studyflow.domain.naegong.repository.NaegongHistoryRepository;
import com.studyflow.domain.naegong.repository.NaegongHistoryRepository.WeeklyNaegongGain;
import com.studyflow.domain.teacher.dto.HotTeacherResponse;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 메인 홈 "이번주 HOT 선생님" — 지난 7일 내공 획득 상위 선생님 TOP3 (비로그인 공개)
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HotTeacherService {

    private static final int LIMIT = 3;       // 노출 인원
    private static final int WEEK_DAYS = 7;   // 집계 기간(일)
    /** JPQL NOT IN 절에 빈 리스트를 넘길 수 없어 noExclusions=true 분기에서 사용하는 더미값.
     *  noExclusions=true 일 때 JPQL이 short-circuit되므로 실제 SQL 조건에 영향을 주지 않는다. */
    private static final Long EMPTY_EXCLUDE_SENTINEL = -1L;

    private final NaegongHistoryRepository naegongHistoryRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    // 이번주 HOT 선생님 조회 — 주간 획득자 우선, 3명 미만이면 전체기간 내공순으로 채운다(hybrid).
    @Cacheable(CacheConfig.HOT_TEACHERS)
    public List<HotTeacherResponse> getWeeklyHotTeachers() {
        LocalDateTime since = LocalDateTime.now().minusDays(WEEK_DAYS);

        // 1) 주간 내공 획득 상위자 (userId + 합산점수)
        List<WeeklyNaegongGain> gains =
                naegongHistoryRepository.findWeeklyTopGainers(since, PageRequest.of(0, LIMIT));

        // 2) 노출 가능한 선생님 프로필만 일괄 조회 (isListed=false/비활성은 제외)
        List<Long> gainUserIds = gains.stream().map(WeeklyNaegongGain::getUserId).toList();
        Map<Long, TeacherProfile> profileByUserId = gainUserIds.isEmpty()
                ? Map.of()
                : teacherProfileRepository.findListedWithUserByUserIds(gainUserIds).stream()
                        .collect(Collectors.toMap(tp -> tp.getUser().getId(), tp -> tp));

        // 3) 주간 획득 순서를 유지하며 노출 가능한 선생님만 추림
        List<TeacherProfile> ordered = new ArrayList<>();
        Map<Long, Long> weeklyGainByUserId = new LinkedHashMap<>();
        for (WeeklyNaegongGain g : gains) {
            TeacherProfile tp = profileByUserId.get(g.getUserId());
            if (tp != null) {
                ordered.add(tp);
                weeklyGainByUserId.put(g.getUserId(), g.getWeeklyScore());
            }
        }

        // 4) 3명 미만이면 전체기간 내공순으로 채움 (이미 포함된 선생님 제외)
        if (ordered.size() < LIMIT) {
            List<Long> excludeUserIds = ordered.stream().map(tp -> tp.getUser().getId()).toList();
            boolean noExclusions = excludeUserIds.isEmpty();
            List<TeacherProfile> fillers = teacherProfileRepository.findTopByNaegongScore(
                    noExclusions,
                    noExclusions ? List.of(EMPTY_EXCLUDE_SENTINEL) : excludeUserIds,
                    PageRequest.of(0, LIMIT - ordered.size()));
            ordered.addAll(fillers);
        }

        if (ordered.isEmpty()) {
            return List.of();
        }

        // 5) 대표 전문 과목명 일괄 조회 (N+1 방지, 선생님당 첫 과목)
        List<Long> profileIds = ordered.stream().map(TeacherProfile::getId).toList();
        Map<Long, String> subjectByProfileId = teacherProfileRepository
                .findSpecialtySubjectsByTeacherProfileIds(profileIds).stream()
                .collect(Collectors.toMap(
                        TeacherProfileRepository.TeacherSpecialty::getTeacherProfileId,
                        TeacherProfileRepository.TeacherSpecialty::getSubjectName,
                        (first, second) -> first));

        // 6) 응답 변환 + 순위 부여
        List<HotTeacherResponse> result = new ArrayList<>(ordered.size());
        int rank = 1;
        for (TeacherProfile tp : ordered) {
            long weeklyGain = weeklyGainByUserId.getOrDefault(tp.getUser().getId(), 0L);
            result.add(HotTeacherResponse.of(tp, rank++, subjectByProfileId.get(tp.getId()), weeklyGain));
        }
        return result;
    }
}
