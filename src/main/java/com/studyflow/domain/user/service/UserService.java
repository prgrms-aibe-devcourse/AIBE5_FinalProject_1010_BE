package com.studyflow.domain.user.service;

import com.studyflow.domain.auth.exception.InvalidCredentialsException;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.exception.DeleteAdminException;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.auth.JwtTokenProvider;
import com.studyflow.global.exception.ErrorCode;
import com.studyflow.global.redis.RedisPrefixProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    // 회원 탈퇴 로직
    public void deleteUser(Long userId) {
        // userId에 해당하는, 삭제되지 않은 User 존재하는지 확인
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 관리자 탈퇴는 불가능
        if(user.getRole() == UserRole.ADMIN) {
           throw new DeleteAdminException(ErrorCode.ACCESS_DENIED, "탈퇴가 불가능한 계정입니다.");
        }

        // user 객체의 isDeleted를 PK 값으로 변경
        user.deleteUser();

        // 변경된 user 상태를 DB에 저장
        userRepository.save(user);

        // Redis에서 refresh token 삭제: key = rt:{userId}
        redisTemplate.delete(RedisPrefixProvider.refreshTokenKey(userId));
    }
}
