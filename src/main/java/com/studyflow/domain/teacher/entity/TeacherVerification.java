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
@Table(name = "teacher_verification")
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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String rejectedReason;

    public static TeacherVerification create(User user, DocumentType documentType, String documentUrl, String description) {
        TeacherVerification v = new TeacherVerification();
        v.user = user;
        v.documentType = documentType;
        v.documentUrl = documentUrl;
        v.description = description;
        return v;
    }
}
