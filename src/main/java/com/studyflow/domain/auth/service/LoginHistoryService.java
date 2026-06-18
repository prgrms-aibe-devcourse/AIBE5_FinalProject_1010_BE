package com.studyflow.domain.auth.service;

import com.studyflow.domain.auth.dto.LoginHistoryResponse;
import com.studyflow.domain.auth.entity.LoginHistory;
import com.studyflow.domain.auth.repository.LoginHistoryRepository;
import com.studyflow.global.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LoginHistoryService {

    private static final int PAGE_SIZE = 12;
    private static final int HISTORY_DAYS = 30;

    private final LoginHistoryRepository loginHistoryRepository;

    // REQUIRES_NEW: 호출자(AuthService, SocialSignupService) 트랜잭션과 분리해
    // 저장 실패 시 메인 트랜잭션의 rollback-only 오염을 방지
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String ipAddress, String userAgent) {
        try {
            loginHistoryRepository.save(LoginHistory.of(
                    userId,
                    ipAddress,
                    UserAgentParser.extractDeviceInfo(userAgent),
                    UserAgentParser.extractBrowser(userAgent)
            ));
        } catch (Exception e) {
            log.error("로그인 기록 저장 실패 — userId={}, cause={}", userId, e.getMessage(), e);
        }
    }

    public Page<LoginHistoryResponse> getMyLoginHistory(Long userId, int page) {
        LocalDateTime from = LocalDateTime.now().minusDays(HISTORY_DAYS);
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);
        return loginHistoryRepository
                .findByUserIdAndLoginAtAfterOrderByLoginAtDesc(userId, from, pageable)
                .map(LoginHistoryResponse::from);
    }
}
