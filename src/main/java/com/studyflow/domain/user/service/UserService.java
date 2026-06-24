package com.studyflow.domain.user.service;

import com.studyflow.domain.enrollment.service.EnrollmentService;
import com.studyflow.domain.user.dto.PasswordChangeRequest;
import com.studyflow.domain.user.dto.UserUpdateRequest;
import com.studyflow.domain.user.dto.UserInfoResponse;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.exception.DeleteAdminException;
import com.studyflow.domain.user.exception.InvalidUserUpdateException;
import com.studyflow.domain.user.exception.TeacherHasActiveStudentsException;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import com.studyflow.global.redis.RedisPrefixProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    // 회원정보 수정 로직
    public void updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // gender 문자열 → enum 변환
        Gender gender;
        try {
            gender = Gender.valueOf(request.getGender().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidUserUpdateException(ErrorCode.VALIDATION_ERROR,
                    "유효하지 않은 성별입니다: " + request.getGender());
        }

        // birthDate 문자열 → LocalDate 변환 (STRICT: 2001-02-30 같은 존재하지 않는 날짜도 거부)
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(request.getBirthDate(),
                    DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            throw new InvalidUserUpdateException(ErrorCode.VALIDATION_ERROR,
                    "유효하지 않은 날짜입니다: " + request.getBirthDate());
        }

        user.updateUser(request.getName(), request.getPhone(), gender,
                birthDate, request.getMarketingAgreed(), request.getProfileImageUrl());
    }

    // 보이스톡 수신 설정 변경 로직
    public void updateVoiceCallSetting(Long userId, boolean enabled) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        user.updateVoiceCallEnabled(enabled);
    }

    // 비밀번호 변경 로직
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 소셜 로그인 사용자는 비밀번호 없음 → 변경 불가
        if (user.getSocialProvider() != SocialProvider.LOCAL) {
            throw new InvalidUserUpdateException(ErrorCode.VALIDATION_ERROR, "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        // 기존 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidUserUpdateException(ErrorCode.VALIDATION_ERROR, "기존 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 기존 비밀번호와 동일한 경우 차단
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidUserUpdateException(ErrorCode.VALIDATION_ERROR, "새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        // 새 비밀번호와 새 비밀번호 확인 일치 확인
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new InvalidUserUpdateException(ErrorCode.VALIDATION_ERROR, "새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    // 회원 정보 조회
    @Transactional(readOnly = true)
    public UserInfoResponse getUser(Long userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return new UserInfoResponse(user);
    }

    // 회원 탈퇴 로직
    public void deleteUser(Long userId) {
        // userId에 해당하는, 삭제되지 않은 User 존재하는지 확인
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 관리자 탈퇴는 불가능
        if(user.getRole() == UserRole.ADMIN) {
           throw new DeleteAdminException(ErrorCode.ACCESS_DENIED, "탈퇴가 불가능한 계정입니다.");
        }

        // 선생님인 경우, RECRUITING/IN_PROGRESS 수업에 ACTIVE 수강생이 있으면 탈퇴 불가
        if (user.getRole() == UserRole.TEACHER && enrollmentService.hasActiveStudentsForTeacher(userId)) {
            throw new TeacherHasActiveStudentsException(ErrorCode.TEACHER_HAS_ACTIVE_STUDENTS,
                    ErrorCode.TEACHER_HAS_ACTIVE_STUDENTS.getMessage());
        }

        // user 객체의 isDeleted를 PK 값으로 변경
        user.deleteUser();

        // Redis에서 refresh token 삭제: key = rt:{userId}
        redisTemplate.delete(RedisPrefixProvider.refreshTokenKey(userId));
    }
}
