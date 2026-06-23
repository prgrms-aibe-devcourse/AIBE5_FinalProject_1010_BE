package com.studyflow.domain.chat.controller;

import com.studyflow.domain.chat.dto.request.ChatRoomCreateRequest;
import com.studyflow.domain.chat.dto.request.ChatReadRequest;
import com.studyflow.domain.chat.dto.request.CourseGroupChatParticipantRequest;
import com.studyflow.domain.chat.dto.request.CourseGroupChatRoomCreateRequest;
import com.studyflow.domain.chat.dto.response.ChatMessagePageResponse;
import com.studyflow.domain.chat.dto.response.ChatReadResponse;
import com.studyflow.domain.chat.dto.response.ChatRoomResponse;
import com.studyflow.domain.chat.service.ChatService;
import com.studyflow.domain.chat.service.CourseGroupChatService;
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
    private final CourseGroupChatService courseGroupChatService;

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
     * 수업 단체톡 생성 또는 기존 방 반환.
     *
     * 담당 선생님만 호출할 수 있고, studentIds가 비어 있으면 현재 ACTIVE 수강생 전체를 초대한다.
     */
    @PostMapping("/course-group")
    public ChatRoomResponse createCourseGroupRoom(
            @Valid @RequestBody CourseGroupChatRoomCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return courseGroupChatService.createCourseGroupRoom(userId, request);
    }

    /**
     * 수업 단체톡에 학생 초대.
     */
    @PostMapping("/{roomId}/participants")
    public ChatRoomResponse inviteCourseGroupStudents(
            @PathVariable Long roomId,
            @Valid @RequestBody CourseGroupChatParticipantRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return courseGroupChatService.inviteCourseGroupStudents(userId, roomId, request);
    }

    /**
     * 수업 단체톡에서 학생 내보내기.
     */
    @DeleteMapping("/{roomId}/participants/{studentId}")
    public ChatRoomResponse removeCourseGroupStudent(
            @PathVariable Long roomId,
            @PathVariable Long studentId,
            @AuthenticationPrincipal Long userId
    ) {
        return courseGroupChatService.removeCourseGroupStudent(userId, roomId, studentId);
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

    /**
     * 메시지 읽음 처리.
     *
     * WebSocket 읽음 처리와 같은 저장 로직을 사용한다. 소켓 연결 전 방을 열어도 읽음 상태가 누락되지 않도록
     * 프론트에서 안전한 동기화 경로로 호출할 수 있다.
     */
    @PatchMapping("/{roomId}/read")
    public ChatReadResponse readMessages(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatReadRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return chatService.readUpTo(userId, roomId, request.getLastReadMessageId());
    }
}
