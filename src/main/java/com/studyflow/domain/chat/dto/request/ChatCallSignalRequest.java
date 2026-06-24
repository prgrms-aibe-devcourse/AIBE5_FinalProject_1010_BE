package com.studyflow.domain.chat.dto.request;

import com.studyflow.domain.chat.enums.ChatCallSignalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 보이스톡 WebRTC 신호 요청.
 *
 * sdp: OFFER/ANSWER에서 RTCSessionDescriptionInit 객체
 * iceCandidate: ICE에서 RTCIceCandidateInit 객체
 */
@Data
@NoArgsConstructor
public class ChatCallSignalRequest {

    @NotNull
    private ChatCallSignalType type;

    @NotBlank
    private String callId;

    private Long targetUserId;

    private Map<String, Object> sdp;

    private Map<String, Object> iceCandidate;

    private String reason;
}
