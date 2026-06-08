package com.studyflow.domain.naegong.service;

import com.studyflow.domain.naegong.entity.NaegongHistory;
import com.studyflow.domain.naegong.enums.NaegongReason;
import com.studyflow.domain.naegong.repository.NaegongHistoryRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NaegongService} 단위 테스트.
 *
 * <p>선생님 프로필 점수 증가와 이력 저장을 외부 의존 없이 확인한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class NaegongServiceTest {

    @Mock TeacherProfileRepository teacherProfileRepository;
    @Mock NaegongHistoryRepository naegongHistoryRepository;

    @InjectMocks NaegongService service;

    private User mockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    @Test
    @DisplayName("addScore: 이력을 저장하고 선생님 프로필 누적 점수를 증가시킨 뒤 누적값을 반환한다")
    void addScore_savesHistoryAndIncrementsProfile() {
        User teacher = mockUser(2L);
        TeacherProfile profile = TeacherProfile.createForUser(teacher); // naegongScore=0
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));

        int total = service.addScore(teacher, 10, NaegongReason.ANSWER_ACCEPTED, 20L);

        assertThat(total).isEqualTo(10);
        assertThat(profile.getNaegongScore()).isEqualTo(10);
        verify(naegongHistoryRepository).save(any(NaegongHistory.class));
    }

    @Test
    @DisplayName("addScore: 선생님 프로필이 없으면 IllegalStateException, 이력도 저장하지 않는다(상태 일관성)")
    void addScore_noProfile_throwsAndSkipsHistory() {
        User teacher = mockUser(2L);
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addScore(teacher, 10, NaegongReason.ANSWER_ACCEPTED, 20L))
                .isInstanceOf(IllegalStateException.class);

        verify(naegongHistoryRepository, never()).save(any(NaegongHistory.class));
    }
}
