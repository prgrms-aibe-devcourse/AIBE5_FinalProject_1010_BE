package com.studyflow.domain.classroom.dto.response;

/**
 * 비수강생 미리보기 토큰 발급 응답.
 *
 * <p>프론트는 livekitUrl + token으로 연결한 뒤, hostIdentity 참가자(선생님)의 트랙만 렌더링한다
 * (학생 프라이버시 보호). previewSeconds 동안만 시청 가능하며 그 후 자동 종료된다.</p>
 */
public record LivekitPreviewTokenResponse(
        String livekitUrl,
        String roomName,
        String token,
        String identity,
        String displayName,
        String hostIdentity,   // "user-{teacherUserId}" — FE가 이 참가자 트랙만 보여줌
        int previewSeconds     // 미리보기 허용 시간(초) — FE 카운트다운 단일 출처
) {
}
