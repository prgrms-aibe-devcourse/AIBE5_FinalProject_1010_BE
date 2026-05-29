package com.studyflow.domain.chat.controller;

import com.studyflow.domain.chat.dto.request.ChatRoomCreateRequest;
import com.studyflow.domain.chat.dto.response.ChatMessagePageResponse;
import com.studyflow.domain.chat.dto.response.ChatRoomResponse;
import com.studyflow.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;

    /**
     * 내 채팅방 목록 조회.
     *
     * 현재는 1:1 매칭 문의 채팅방만 조회된다.
     * 나중에 단체 채팅을 추가해도 participant 구조를 쓰기 때문에 같은 API를 확장해서 사용할 수 있다.
     */
    @GetMapping
    public List<ChatRoomResponse> getMyChatRooms(
            @AuthenticationPrincipal Long userId
    ) {
        return chatService.getMyChatRooms(userId);
    }

    /**
     * 선생님-학생 1:1 매칭 문의 채팅방 생성.
     *
     * 요청 예시:
     * {
     *   "teacherId": 3,
     *   "studentId": 8
     * }
     *
     * 같은 선생님/학생 조합의 채팅방이 이미 있으면 기존 채팅방을 반환한다.
     */
    @PostMapping("/direct")
    public ChatRoomResponse createDirectRoom(
            @Valid @RequestBody ChatRoomCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return chatService.createDirectRoom(userId, request);
    }

    /**
     * 메시지 목록 조회.
     *
     * cursor가 없으면 최신 메시지부터 조회한다.
     * cursor가 있으면 해당 메시지 ID보다 오래된 메시지를 조회한다.
     */
    @GetMapping("/{roomId}/messages")
    public ChatMessagePageResponse getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal Long userId
    ) {
        return chatService.getMessages(
                userId,
                roomId,
                cursor,
                size
        );
    }
}
