package com.studyflow.domain.subject.entity;

import com.studyflow.domain.subject.enums.SubjectCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Subject} 엔티티 단위 테스트.
 *
 * <p>대분류 과목을 만드는 정적 팩토리 {@link Subject#ofCategory}의 동작을 검증한다.</p>
 */
class SubjectTest {

    @Test
    @DisplayName("ofCategory는 표시명을 이름으로, 분류를 category로 설정하고 부모는 두지 않는다")
    void ofCategory_setsNameAndCategoryWithoutParent() {
        Subject subject = Subject.ofCategory(SubjectCategory.MATH);

        assertThat(subject.getName()).isEqualTo("수학");
        assertThat(subject.getCategory()).isEqualTo(SubjectCategory.MATH);
        assertThat(subject.getParentSubject()).isNull();
        assertThat(subject.getId()).isNull(); // 영속화 전이므로 id는 아직 없다
    }

    @Test
    @DisplayName("분류마다 이름이 해당 분류의 표시명과 일치한다")
    void ofCategory_nameMatchesDisplayName() {
        for (SubjectCategory category : SubjectCategory.values()) {
            Subject subject = Subject.ofCategory(category);
            assertThat(subject.getName()).isEqualTo(category.getDisplayName());
            assertThat(subject.getCategory()).isEqualTo(category);
        }
    }
}
