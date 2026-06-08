package com.studyflow.domain.naegong.entity;

import com.studyflow.domain.naegong.enums.NaegongReason;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내공 점수 변동 이력. (apidetail.md 20. Naegong API의 naegong_history 테이블)
 *
 * <p>TeacherProfile.naegongScore가 "현재 점수"라면, 이 엔티티는 변동의 감사 로그다.
 * 변동을 발생시킨 대상은 {@code referenceId}로 참조한다(예: 채택된 답변 id).</p>
 */
@Getter
@Entity
@Table(name = "naegong_history", indexes = {
        @Index(name = "idx_naegong_history_user_created", columnList = "user_id, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NaegongHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 점수를 받은 사용자 (선생님)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "score_change", nullable = false)
    private int scoreChange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NaegongReason reason;

    // 변동을 유발한 대상의 식별자 (예: 채택된 답변 id). 사유별로 의미가 다르다.
    @Column(name = "reference_id")
    private Long referenceId;

    public static NaegongHistory create(User user, int scoreChange, NaegongReason reason, Long referenceId) {
        NaegongHistory h = new NaegongHistory();
        h.user = user;
        h.scoreChange = scoreChange;
        h.reason = reason;
        h.referenceId = referenceId;
        return h;
    }
}
