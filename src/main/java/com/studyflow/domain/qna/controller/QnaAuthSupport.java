package com.studyflow.domain.qna.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** QnA 컨트롤러 공용 인증 헬퍼. */
final class QnaAuthSupport {

    private QnaAuthSupport() {
    }

    /** 현재 인증 주체가 ADMIN 권한을 가지는지 여부. (작성자가 아니어도 삭제 가능한 모더레이션용) */
    static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
