package com.studyflow.domain.naegong.service;

import com.studyflow.domain.naegong.entity.NaegongHistory;
import com.studyflow.domain.naegong.enums.NaegongReason;
import com.studyflow.domain.naegong.repository.NaegongHistoryRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        TeacherProfile profile = teacherProfileRepository.findByUserId(teacher.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "내공 적립 대상 선생님 프로필이 없습니다. userId=" + teacher.getId()));

        naegongHistoryRepository.save(NaegongHistory.create(teacher, scoreChange, reason, referenceId));
        return profile.addNaegongScore(scoreChange);
    }
}
