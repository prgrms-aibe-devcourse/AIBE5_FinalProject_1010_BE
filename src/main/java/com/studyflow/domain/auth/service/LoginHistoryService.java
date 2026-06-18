package com.studyflow.domain.auth.service;

import com.studyflow.domain.auth.dto.LoginHistoryResponse;
import com.studyflow.domain.auth.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LoginHistoryService {

    private static final int PAGE_SIZE = 12;
    private static final int HISTORY_DAYS = 30;

    private final LoginHistoryRepository loginHistoryRepository;

    public Page<LoginHistoryResponse> getMyLoginHistory(Long userId, int page) {
        LocalDateTime from = LocalDateTime.now().minusDays(HISTORY_DAYS);
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);
        return loginHistoryRepository
                .findByUserIdAndLoginAtAfterOrderByLoginAtDesc(userId, from, pageable)
                .map(LoginHistoryResponse::from);
    }
}
