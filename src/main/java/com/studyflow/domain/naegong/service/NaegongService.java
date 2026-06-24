package com.studyflow.domain.naegong.service;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.naegong.dto.NaegongHistoryItem;
import com.studyflow.domain.naegong.dto.NaegongHistoryPageResponse;
import com.studyflow.domain.naegong.entity.NaegongHistory;
import com.studyflow.domain.naegong.enums.NaegongReason;
import com.studyflow.domain.naegong.repository.NaegongHistoryRepository;
import com.studyflow.domain.qna.entity.QnaAnswer;
import com.studyflow.domain.qna.repository.QnaAnswerRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 내공 점수 적립/이력 도메인 서비스.
 *
 * <p>QnA 답변 채택 등 다른 도메인에서 호출되어, 선생님의 누적 내공 점수
 * ({@link TeacherProfile#getNaegongScore()})를 갱신하고 {@link NaegongHistory} 감사 로그를 남긴다.</p>
 */
@Service
@RequiredArgsConstructor
public class NaegongService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final NaegongHistoryRepository naegongHistoryRepository;
    private final QnaAnswerRepository qnaAnswerRepository;
    private final CourseRepository courseRepository;

    /**
     * 화상수업 종료 시 선생님 내공 적립 — 시간 기반 계산 + 일일 한도(courseId 기준 하루 최대 50점).
     *
     * <p>10분당 +10점, 10분 미만은 0점, 1회 최대 50점. 한국 날짜 기준으로 같은 수업에 이미 지급된
     * 합계를 차감해 잔여 한도만큼만 지급한다. 0점이면 이력을 남기지 않고 조기 반환한다.</p>
     */
    @Transactional
    public void addScoreForClassroomSession(User teacher, Long courseId, long durationSeconds) {
        int durationMinutes = (int) (durationSeconds / 60);
        int score = (durationMinutes / 10) * 10;
        score = Math.min(score, 50);

        if (score <= 0) {
            return;
        }

        // TeacherProfile 비관적 락 획득 → 이후 일일 합계 조회까지 직렬화
        TeacherProfile profile = teacherProfileRepository.findByUserIdWithLock(teacher.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "내공 적립 대상 선생님 프로필이 없습니다. userId=" + teacher.getId()));

        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        LocalDateTime startOfDay = LocalDate.now(seoulZone)
                .atStartOfDay(seoulZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        int todayPaid = naegongHistoryRepository.sumScoreByUserIdAndReasonAndCourseIdSince(
                teacher.getId(), NaegongReason.CLASSROOM_SESSION_CLOSED, courseId, startOfDay);

        int finalScore = Math.min(score, 50 - todayPaid);
        if (finalScore <= 0) {
            return;
        }

        naegongHistoryRepository.save(
                NaegongHistory.create(teacher, finalScore, NaegongReason.CLASSROOM_SESSION_CLOSED, courseId));
        profile.addNaegongScore(finalScore);
    }

    /**
     * 선생님의 현재 누적 내공 점수를 조회한다(읽기 전용 — 점수 변동 없음).
     * 내공 지급이 스킵된 경우에도 응답에 정확한 누적값을 내려줘야 할 때 사용한다.
     * TeacherProfile이 없는 경우(비정상 상태) 0을 반환한다.
     */
    @Transactional(readOnly = true)
    public int getCurrentScore(Long userId) {
        return teacherProfileRepository.findByUserId(userId)
                .map(TeacherProfile::getNaegongScore)
                .orElse(0);
    }

    /**
     * 선생님 마이페이지 내공 탭 — 내공 변동 이력을 페이지 단위로 반환한다.
     *
     * <p>totalScore는 TeacherProfile의 현재 누적 내공이다.
     * relatedTitle은 reason별로 다른 엔티티를 참조하므로 페이지 내 id를 모아 일괄 조회(N+1 방지)한다.
     * 참조 대상이 삭제된 경우 relatedTitle은 null로 반환한다.</p>
     */
    @Transactional(readOnly = true)
    public NaegongHistoryPageResponse getHistoryPage(Long userId, Pageable pageable) {
        int totalScore = teacherProfileRepository.findByUserId(userId)
                .map(TeacherProfile::getNaegongScore)
                .orElse(0);

        Page<NaegongHistory> historyPage = naegongHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        Set<Long> answerIds = historyPage.getContent().stream()
                .filter(h -> h.getReason() == NaegongReason.ANSWER_ACCEPTED)
                .map(NaegongHistory::getReferenceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> courseIds = historyPage.getContent().stream()
                .filter(h -> h.getReason() == NaegongReason.CLASSROOM_SESSION_CLOSED)
                .map(NaegongHistory::getReferenceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> answerTitleMap = answerIds.isEmpty() ? Map.of()
                : qnaAnswerRepository.findAllWithQuestionByIdIn(answerIds).stream()
                        .collect(Collectors.toMap(QnaAnswer::getId, a -> a.getQuestion().getTitle()));
        Map<Long, String> courseTitleMap = courseIds.isEmpty() ? Map.of()
                : courseRepository.findAllById(courseIds).stream()
                        .collect(Collectors.toMap(Course::getId, Course::getTitle));

        Page<NaegongHistoryItem> itemPage = historyPage.map(h -> {
            String title = switch (h.getReason()) {
                case ANSWER_ACCEPTED -> answerTitleMap.get(h.getReferenceId());
                case CLASSROOM_SESSION_CLOSED -> courseTitleMap.get(h.getReferenceId());
            };
            return NaegongHistoryItem.of(h, title);
        });

        return NaegongHistoryPageResponse.of(totalScore, itemPage);
    }

    /**
     * 선생님에게 내공 점수를 적립하고 이력을 기록한다.
     *
     * <p>프로필을 먼저 확인한 뒤 이력을 저장한다. 선생님인데 프로필이 없는 것은 본래 불가능한
     * 비정상 상태이므로 예외로 처리해, 이력만 남고 점수는 미적립되는 상태 불일치를 막는다.
     * (같은 트랜잭션이므로 예외 발생 시 이력 INSERT도 함께 롤백된다.)</p>
     *
     * @param teacher     점수를 받을 사용자(선생님)
     * @param scoreChange 점수 변동량(양수면 적립)
     * @param reason      변동 사유
     * @param referenceId 변동을 유발한 대상 id (예: 채택된 답변 id)
     * @return 변동 후 누적 내공 점수
     */
    @Transactional
    public int addScore(User teacher, int scoreChange, NaegongReason reason, Long referenceId) {
        TeacherProfile profile = teacherProfileRepository.findByUserIdWithLock(teacher.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "내공 적립 대상 선생님 프로필이 없습니다. userId=" + teacher.getId()));

        naegongHistoryRepository.save(NaegongHistory.create(teacher, scoreChange, reason, referenceId));
        return profile.addNaegongScore(scoreChange);
    }
}
