package com.studyflow.global.auth;

import com.studyflow.global.exception.AuthException;
import com.studyflow.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import io.jsonwebtoken.Claims;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final com.studyflow.global.config.PublicUrlProvider publicUrlProvider;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        for (String publicUrl : publicUrlProvider.getPublicUrls()) {
            if (PATH_MATCHER.match(publicUrl, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // refresh token 기반 인증 (/api/v1/auth/reissue 전용)
        if (request.getRequestURI().equals("/api/v1/auth/reissue")) {
            // HttpOnly 쿠키에 있는 refresh token을 가져오기
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            String refreshToken = null;
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie c : cookies) {
                    if ("refreshToken".equals(c.getName())) {
                        refreshToken = c.getValue();
                        break;
                    }
                }
            }

            if (!StringUtils.hasText(refreshToken)) {
                // 쿠키에 토큰이 없으면 인증 실패 응답
                writeAuthErrorResponse(response, ErrorCode.AUTH_INVALID_TOKEN);
                return;
            }

            try {
                // 토큰 검증 및 Claims 추출
                Claims claims = jwtTokenProvider.validateAndGetClaims(refreshToken);

                // 토큰 타입이 refresh인지 확인
                String type = jwtTokenProvider.getTypeFromClaims(claims);
                if (!"refresh".equals(type)) {
                    throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN, "Not a refresh token");
                }

                // Claims에서 사용자 정보 추출 후 인증 객체 설정
                Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
                String role = jwtTokenProvider.getRoleFromClaims(claims);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // refresh 전용 로직이므로 여기서 필터 체인을 진행하고 리턴
                filterChain.doFilter(request, response);
                return;
            } catch (AuthException e) {
                SecurityContextHolder.clearContext();
                writeAuthErrorResponse(response, e.getErrorCode());
                return;
            }
        }

        // Authorization 헤더에 토큰이 없을 경우 조기 리턴
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // access token 기반 인증
        try {
            Claims claims = jwtTokenProvider.validateAndGetClaims(token);

            // 토큰이 access인지 확인 (refresh 토큰이면 실패)
            String type = jwtTokenProvider.getTypeFromClaims(claims);
            if (!"access".equals(type)) {
                throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN, "Not an access token");
            }

            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (AuthException e) {
            SecurityContextHolder.clearContext();
            writeAuthErrorResponse(response, e.getErrorCode());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private void writeAuthErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":\"%s\",\"message\":\"%s\"}", errorCode.name(), errorCode.getMessage()));
    }
}
