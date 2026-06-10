package com.studyflow.domain.teacher.entity;

import com.studyflow.domain.teacher.enums.DocumentType;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "teacher_verification",
        // (user_id, is_processed) unique 제약 — PENDING 중복 방지 전략
        // PENDING 상태: is_processed = 0 → (user_id, 0) 조합이 유일 → 동시 요청 Race Condition 차단
        // APPROVED/REJECTED 상태: is_processed = 자신의 PK → 모두 고유값 → 여러 처리 이력 허용
        uniqueConstraints = @UniqueConstraint(
                name = "uk_teacher_verification_user_processed",
                columnNames = {"user_id", "is_processed"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherVerification extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(length = 500, nullable = false)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    /**
     * PENDING 중복 방지용 sentinel 컬럼.
     * PENDING : 0  → (user_id, 0) unique 제약으로 동시 중복 신청 차단
     * APPROVED/REJECTED : 자신의 PK → 고유값이므로 여러 처리 이력 공존 가능
     */
    @Column(name = "is_processed", nullable = false)
    private Long isProcessed = 0L;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String awards;

    @Column(columnDefinition = "TEXT")
    private String career;

    @Column(length = 300)
    private String education;

    @Column(columnDefinition = "TEXT")
    private String rejectedReason;

    // 관리자가 수락 또는 거절 처리 시 호출 — isProcessed를 자신의 PK로 설정해 unique 제약 해제
    public void process(VerificationStatus newStatus, String rejectedReason) {
        this.status = newStatus;
        this.isProcessed = this.id;
        this.reviewedAt = LocalDateTime.now();
        if (newStatus == VerificationStatus.REJECTED) {
            this.rejectedReason = rejectedReason;
        }
    }

    public static TeacherVerification create(User user, DocumentType documentType, String documentUrl,
                                             String description, String awards, String career, String education) {
        TeacherVerification v = new TeacherVerification();
        v.user = user;
        v.documentType = documentType;
        v.documentUrl = documentUrl;
        v.description = description;
        v.awards = awards;
        v.career = career;
        v.education = education;
        return v;
    }
}
