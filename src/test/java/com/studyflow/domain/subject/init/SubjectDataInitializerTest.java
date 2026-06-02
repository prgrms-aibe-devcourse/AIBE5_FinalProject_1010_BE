package com.studyflow.domain.subject.init;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.enums.SubjectCategory;
import com.studyflow.domain.subject.repository.SubjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SubjectDataInitializer} 단위 테스트.
 *
 * <p>시딩이 "없는 분류만 생성"하는 멱등 동작인지 확인한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class SubjectDataInitializerTest {

    @Mock
    SubjectRepository subjectRepository;

    @InjectMocks
    SubjectDataInitializer initializer;

    @Test
    @DisplayName("아무 과목도 없으면 8개 대분류를 모두 시딩한다")
    void seedsAllWhenEmpty() {
        when(subjectRepository.existsByCategory(any())).thenReturn(false);

        initializer.run(null);

        ArgumentCaptor<Subject> captor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository, times(8)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Subject::getCategory)
                .containsExactlyInAnyOrder(SubjectCategory.values());
    }

    @Test
    @DisplayName("이미 모든 분류가 있으면 아무것도 새로 만들지 않는다(멱등)")
    void seedsNothingWhenAllExist() {
        when(subjectRepository.existsByCategory(any())).thenReturn(true);

        initializer.run(null);

        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("일부만 없으면 없는 분류만 시딩한다")
    void seedsOnlyMissing() {
        // 수학·국어는 이미 있고 나머지는 없는 상황
        when(subjectRepository.existsByCategory(any())).thenAnswer(invocation -> {
            SubjectCategory category = invocation.getArgument(0);
            return category == SubjectCategory.MATH || category == SubjectCategory.KOREAN;
        });

        initializer.run(null);

        ArgumentCaptor<Subject> captor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository, times(6)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Subject::getCategory)
                .doesNotContain(SubjectCategory.MATH, SubjectCategory.KOREAN);
    }
}
