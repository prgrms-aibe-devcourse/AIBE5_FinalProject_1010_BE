package com.studyflow.domain.subject.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SubjectCategory} 단위 테스트.
 *
 * <p>8개 대분류라는 약속과, 각 분류가 화면 표시명·AI 지침을 빠짐없이 갖는지를 검증한다.
 * (실수로 값이 추가/삭제되거나 지침이 비는 것을 잡아낸다.)</p>
 */
class SubjectCategoryTest {

    @Test
    @DisplayName("수능 과목 대분류는 정확히 8개다")
    void hasExactlyEightCategories() {
        assertThat(SubjectCategory.values()).hasSize(8);
    }

    @Test
    @DisplayName("표시명은 8개 모두 서로 다르다(중복 없음)")
    void displayNamesAreDistinct() {
        long distinct = Arrays.stream(SubjectCategory.values())
                .map(SubjectCategory::getDisplayName)
                .distinct()
                .count();
        assertThat(distinct).isEqualTo(8);
    }

    @ParameterizedTest
    @EnumSource(SubjectCategory.class)
    @DisplayName("모든 분류는 비어 있지 않은 표시명과 AI 지침을 가진다")
    void everyCategoryHasDisplayNameAndGuidance(SubjectCategory category) {
        assertThat(category.getDisplayName()).isNotBlank();
        assertThat(category.getTutorGuidance()).isNotBlank();
    }

    @Test
    @DisplayName("기대하는 8개 표시명을 모두 포함한다")
    void containsExpectedKoreanNames() {
        assertThat(Arrays.stream(SubjectCategory.values()).map(SubjectCategory::getDisplayName).toList())
                .containsExactlyInAnyOrder(
                        "국어", "영어", "수학", "사회탐구", "과학탐구", "직업탐구", "한국사", "제2외국어"
                );
    }
}
