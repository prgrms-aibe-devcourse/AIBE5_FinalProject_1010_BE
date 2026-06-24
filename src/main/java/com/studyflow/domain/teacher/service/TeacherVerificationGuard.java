package com.studyflow.domain.teacher.service;

import com.studyflow.domain.teacher.exception.TeacherNotVerifiedException;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 선생님 전용 기능(수업 등록·수정, 강의실 열기, QnA 답변 등)에서 관리자 인증 여부를 확인하는 공용 가드.
// 미인증(isVerified=false) 선생님이면 TeacherNotVerifiedException(403)을 던진다.
// 인증은 자주 바뀌지 않고 쓰기 작업에서만 호출되므로 매 호출 시 User 단건 조회 비용은 무시할 수준.
@Component
@RequiredArgsConstructor
public class TeacherVerificationGuard {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void requireVerified(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));
        // 선생님이 아닌 경우는 이 가드의 관심사가 아니므로 통과(역할 검증은 각 컨트롤러/시큐리티가 담당)
        if (user.getRole() == UserRole.TEACHER && !user.isVerified()) {
            throw new TeacherNotVerifiedException();
        }
    }
}
