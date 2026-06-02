package com.studyflow.domain.chat.service;

import com.studyflow.domain.chat.dto.request.ChatCallSignalRequest;
import com.studyflow.domain.chat.dto.response.ChatCallSignalResponse;
import com.studyflow.domain.chat.entity.ChatRoom;
import com.studyflow.domain.chat.repository.ChatRoomParticipantRepository;
import com.studyflow.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 채팅방 보이스톡 신호 검증/응답 생성 서비스.
 *
 * 실제 음성 스트림은 WebRTC P2P로 흐르고, 서버는 통화 연결 신호만 중계한다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatCallSignalService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    public ChatCallSignalResponse createSignal(
            Long roomId,
            Long senderId,
            ChatCallSignalRequest request
    ) {
        if (request.getType() == null || request.getCallId() == null || request.getCallId().isBlank()) {
            throw new IllegalArgumentException("보이스톡 신호 타입과 callId는 필수입니다.");
        }

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!chatRoom.isActive()) {
            throw new IllegalArgumentException("종료된 채팅방에서는 보이스톡을 사용할 수 없습니다.");
        }

        if (!chatRoom.isDirectRoom()) {
            throw new IllegalArgumentException("보이스톡은 1:1 채팅방에서만 사용할 수 있습니다.");
        }

        validateParticipant(roomId, senderId);

        if (request.getTargetUserId() != null) {
            if (request.getTargetUserId().equals(senderId)) {
                throw new IllegalArgumentException("자기 자신에게 보이스톡 신호를 보낼 수 없습니다.");
            }
            validateParticipant(roomId, request.getTargetUserId());
        }

        return new ChatCallSignalResponse(
                roomId,
                request.getCallId(),
                request.getType(),
                senderId,
                request.getTargetUserId(),
                request.getSdp(),
                request.getIceCandidate(),
                request.getReason(),
                LocalDateTime.now()
        );
    }

    private void validateParticipant(Long roomId, Long userId) {
        boolean exists = chatRoomParticipantRepository
                .existsByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, userId);

        if (!exists) {
            throw new IllegalArgumentException("채팅방 참여자가 아닙니다.");
        }
    }
}
