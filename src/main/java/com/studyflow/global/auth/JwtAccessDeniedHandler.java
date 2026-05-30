package com.studyflow.global.auth;

import com.studyflow.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//AccessDeniedHandler 구현. 인증은 되었지만 권한이 없는 경우 호출됩니다.
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        ErrorCode ec = ErrorCode.ACCESS_DENIED;
        String body = String.format("{\n  \"success\":false,\n  \"code\":\"%s\",\n  \"message\":\"%s\"\n}", ec.name(), ec.getMessage());
        response.getWriter().write(body);
    }
}
