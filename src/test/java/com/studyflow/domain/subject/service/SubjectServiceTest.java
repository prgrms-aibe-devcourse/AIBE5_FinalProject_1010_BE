package com.studyflow.domain.subject.service;

import com.studyflow.domain.subject.dto.response.SubjectResponse;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.enums.SubjectCategory;
import com.studyflow.domain.subject.repository.SubjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SubjectService} 단위 테스트.
 *
 * <p>리포지토리가 돌려준 과목을 응답 DTO로 올바르게 매핑하는지 확인한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    SubjectRepository subjectRepository;

    @InjectMocks
    SubjectService subjectService;

    @Test
    @DisplayName("최상위 과목을 순서대로 SubjectResponse(subjectId, name, category)로 매핑한다")
    void getSubjects_mapsToResponse() {
        Subject korean = mock(Subject.class);
        when(korean.getId()).thenReturn(1L);
        when(korean.getName()).thenReturn("국어");
        when(korean.getCategory()).thenReturn(SubjectCategory.KOREAN);

        Subject math = mock(Subject.class);
        when(math.getId()).thenReturn(3L);
        when(math.getName()).thenReturn("수학");
        when(math.getCategory()).thenReturn(SubjectCategory.MATH);

        when(subjectRepository.findByParentSubjectIsNullOrderByIdAsc())
                .thenReturn(List.of(korean, math));

        List<SubjectResponse> result = subjectService.getSubjects();

        assertThat(result).containsExactly(
                new SubjectResponse(1L, "국어", "KOREAN"),
                new SubjectResponse(3L, "수학", "MATH")
        );
    }

    @Test
    @DisplayName("category가 없는(null) 과목은 응답의 category도 null이 된다")
    void getSubjects_nullCategoryStaysNull() {
        Subject etc = mock(Subject.class);
        when(etc.getId()).thenReturn(9L);
        when(etc.getName()).thenReturn("기타");
        when(etc.getCategory()).thenReturn(null);

        when(subjectRepository.findByParentSubjectIsNullOrderByIdAsc())
                .thenReturn(List.of(etc));

        List<SubjectResponse> result = subjectService.getSubjects();

        assertThat(result).containsExactly(new SubjectResponse(9L, "기타", null));
    }

    @Test
    @DisplayName("과목이 없으면 빈 목록을 반환한다")
    void getSubjects_empty() {
        when(subjectRepository.findByParentSubjectIsNullOrderByIdAsc()).thenReturn(List.of());

        assertThat(subjectService.getSubjects()).isEmpty();
    }
}
