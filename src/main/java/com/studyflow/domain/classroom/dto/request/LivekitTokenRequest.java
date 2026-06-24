package com.studyflow.domain.classroom.dto.request;

/**
 * LiveKit 토큰 발급(22-4) 요청.
 *
 * <p>deviceType은 클라이언트 구분용(WEB/IOS/ANDROID 등). 현재 토큰 발급에는 영향 없음.</p>
 */
public record LivekitTokenRequest(
        String deviceType
) {
}
