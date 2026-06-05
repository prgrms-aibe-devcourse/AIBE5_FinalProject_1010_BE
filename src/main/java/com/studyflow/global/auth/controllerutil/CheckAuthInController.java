package com.studyflow.global.auth.controllerutil;

import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.function.BiFunction;

public class CheckAuthInController {
    /**
     * 인증 정보가 유효하지 않거나, 권한 불일치 시 예외를 던짐
     *
     * @param userId         JWT에서 추출한 사용자 ID
     * @param authentication Spring Security 인증 객체
     * @param expectedRole   기대하는 역할 (ex. UserRole.STUDENT)
     * @param exceptionFactory (ErrorCode, message) -> T 형태의 예외 생성 함수
     */
    public static <T extends RuntimeException> void checkAuth(
            Long userId,
            Authentication authentication,
            UserRole expectedRole,
            BiFunction<ErrorCode, String, T> exceptionFactory) {

        // 인증 정보가 없는 경우 (NPE 방지)
        if (authentication == null) {
            throw exceptionFactory.apply(ErrorCode.AUTH_REQUIRED, "인증 정보가 유효하지 않습니다.");
        }

        // role 정보 추출
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(null);

        // userId 또는 role이 없는 경우
        if (userId == null || role == null) {
            throw exceptionFactory.apply(ErrorCode.AUTH_REQUIRED, "인증 정보가 유효하지 않습니다.");
        }

        String expectedRoleStr = "ROLE_" + expectedRole.name();

        // 유효한 role이지만 권한 불일치
        if (!role.equals(expectedRoleStr)) {
            // "ROLE_" 접두사를 제거한 뒤 UserRole enum에 존재하는 role인지 확인
            boolean isKnownRole;
            try {
                String roleName = role.startsWith("ROLE_") ? role.substring(5) : role;
                UserRole.valueOf(roleName);
                isKnownRole = true;
            } catch (IllegalArgumentException e) {
                isKnownRole = false;
            }

            if (isKnownRole) {
                throw exceptionFactory.apply(ErrorCode.ACCESS_DENIED, "권한이 없습니다.");
            } else {
                throw exceptionFactory.apply(ErrorCode.AUTH_REQUIRED, "인증 정보가 유효하지 않습니다.");
            }
        }
    }
}
