package com.studyflow.global.auth;

import com.studyflow.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

//AuthenticationEntryPoint 구현. 인증이 필요한 요청에 인증이 없을 때 호출됩니다.
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // 공통 ErrorCode를 사용하여 응답의 code와 message를 일관되게 반환
        ErrorCode ec = ErrorCode.AUTH_REQUIRED;
        String body = String.format("{\n  \"success\":false,\n  \"code\":\"%s\",\n  \"message\":\"%s\"\n}", ec.name(), ec.getMessage());
        response.getWriter().write(body);
    }
}
