package com.studyflow.domain.classroom.service;

import com.studyflow.domain.classroom.dto.response.LiveClassroomResponse;
import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;
import com.studyflow.domain.classroom.repository.ClassroomParticipantRepository;
import com.studyflow.domain.classroom.repository.ClassroomParticipantRepository.SessionParticipantCount;
import com.studyflow.domain.classroom.repository.ClassroomSessionRepository;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 메인 홈 "실시간 강의중" 공개 목록 조회 서비스.
 *
 * <p>지금 OPEN이면서 호스트 하트비트가 살아있는(좀비 아님) + 노출 수업(isListed)인 강의실만 반환한다.
 * 비로그인 포함 공개이며, 비수강생은 이 목록의 카드로 30~60초 영상 미리보기에 진입한다.</p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LiveClassroomQueryService {

    /** 호스트 하트비트가 이 시간 안에 있어야 "실시간"으로 노출 (자동종료 한계 5분과 동일). */
    private static final long FRESH_SECONDS = 5 * 60;
    /** 홈에 한 번에 노출할 최대 개수. */
    private static final int MAX_RESULTS = 12;

    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomParticipantRepository participantRepository;

    public List<LiveClassroomResponse> getLiveClassrooms() {
        LocalDateTime freshAfter = LocalDateTime.now().minusSeconds(FRESH_SECONDS);

        List<ClassroomSession> sessions =
                sessionRepository.findLiveSessions(ClassroomStatus.OPEN, freshAfter);
        if (sessions.isEmpty()) {
            return List.of();
        }
        if (sessions.size() > MAX_RESULTS) {
            sessions = sessions.subList(0, MAX_RESULTS);
        }

        List<Long> sessionIds = sessions.stream().map(ClassroomSession::getId).toList();
        Map<Long, Long> countBySessionId = participantRepository
                .countParticipantsBySessionIds(sessionIds).stream()
                .collect(Collectors.toMap(
                        SessionParticipantCount::getSessionId,
                        SessionParticipantCount::getCount));

        return sessions.stream().map(s -> {
            Course course = s.getCourse();
            TeacherProfile tp = course.getTeacherProfile();
            return new LiveClassroomResponse(
                    s.getId(),
                    course.getId(),
                    course.getTitle(),
                    course.getSubject().getName(),
                    tp.getId(),
                    tp.getUser().getName(),
                    tp.getUser().getProfileImageUrl(),
                    countBySessionId.getOrDefault(s.getId(), 0L).intValue(),
                    s.getStartedAt());
        }).toList();
    }
}
