package com.studyflow.domain.classroom.dto.response;

/**
 * LiveKit 토큰 발급(22-4) 응답.
 *
 * <p>프론트는 livekitUrl + token으로 LiveKit 서버에 연결한다.
 * roomName은 "course-{courseId}-session-{sessionId}" 규칙.</p>
 */
public record LivekitTokenResponse(
        String livekitUrl,
        String roomName,
        String token,
        String identity,
        String displayName,
        String role
) {
}
