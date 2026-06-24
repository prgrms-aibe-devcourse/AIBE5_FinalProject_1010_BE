package com.studyflow.domain.subject.entity;

import com.studyflow.domain.subject.enums.SubjectCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subject")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_subject_id")
    private Subject parentSubject;

    @Column(length = 50, nullable = false)
    private String name;

    /**
     * 수능 기준 과목 대분류. 8개 최상위 과목(부모 없음)은 이 값을 가지며,
     * AI 질문 시 과목별 맞춤 답변 지침을 결정하는 근거가 된다.
     * (향후 세부 과목을 자식으로 둘 경우 category는 null일 수 있다.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private SubjectCategory category;

    /**
     * 수능 8개 대분류 과목(최상위)을 생성하는 정적 팩토리.
     * 이름은 분류의 한글 표시명을 사용하고, 부모 과목은 두지 않는다.
     */
    public static Subject ofCategory(SubjectCategory category) {
        Subject subject = new Subject();
        subject.name = category.getDisplayName();
        subject.category = category;
        return subject;
    }
}
