package com.studyflow.domain.chat.service;

import com.studyflow.domain.chat.dto.request.ChatMessageSendRequest;
import com.studyflow.domain.chat.dto.request.ChatRoomCreateRequest;
import com.studyflow.domain.chat.dto.response.*;
import com.studyflow.domain.chat.entity.ChatMessage;
import com.studyflow.domain.chat.entity.ChatMessageAttachment;
import com.studyflow.domain.chat.entity.ChatRoom;
import com.studyflow.domain.chat.entity.ChatRoomParticipant;
import com.studyflow.domain.chat.enums.ChatMessageType;
import com.studyflow.domain.chat.repository.ChatMessageAttachmentRepository;
import com.studyflow.domain.chat.repository.ChatMessageRepository;
import com.studyflow.domain.chat.repository.ChatRoomParticipantRepository;
import com.studyflow.domain.chat.repository.ChatRoomRepository;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageAttachmentRepository chatMessageAttachmentRepository;
    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;

    /**
     * 자기 자신을 프록시 경유로 호출하기 위한 참조.
     *
     * 같은 빈 안에서 메서드를 직접 호출하면 Spring 트랜잭션 프록시를 타지 않아 @Transactional 이 적용되지 않는다.
     * 채팅방 생성 동시성 처리에서 "조회"와 "생성"을 각각 별도 트랜잭션으로 실행해야 하므로
     * selfProvider 로 프록시 빈을 받아 호출한다. (ObjectProvider 라 지연 조회 → 순환 의존 없음)
     */
    private final ObjectProvider<ChatService> selfProvider;

    /**
     * 선생님-학생 1:1 매칭 문의 채팅방 생성.
     *
     * 지금은 Course가 확정되기 전, 선생님과 학생이 서로 조건을 맞춰보는 문의 채팅만 만든다.
     * 그래서 request.courseId는 받더라도 사용하지 않고, teacherId/studentId 조합만으로 roomKey를 만든다.
     *
     * 이미 같은 teacherId/studentId 조합의 채팅방이 있으면 새로 만들지 않고 기존 방을 반환한다.
     */
    /*
     * 동시성 처리:
     * room_key 에는 DB UNIQUE 제약(uk_chat_room_room_key)이 있어 같은 키의 방이 행으로 두 개 만들어지는 일은 없다.
     * 다만 동시에 두 요청이 들어오면 둘 다 findByRoomKey 에서 empty 를 보고 INSERT 를 시도하는데,
     * 먼저 커밋한 쪽만 성공하고 진 쪽은 DataIntegrityViolationException 을 받는다.
     * 그 경우 새 트랜잭션으로 기존 방을 다시 조회해서 반환한다.
     *
     * 이 메서드는 NOT_SUPPORTED 로 트랜잭션 없이 실행하고, 실제 조회/생성은 self(프록시) 의 별도 트랜잭션 메서드로 위임한다.
     * 그래야 경합에서 진 뒤의 재조회가 '새 스냅샷'에서 이뤄져 먼저 커밋된 방을 볼 수 있다.
     * (같은 트랜잭션 안에서 재조회하면 MySQL REPEATABLE READ 스냅샷 때문에 그 방이 안 보일 수 있다.)
     *
     * 동시 INSERT 가 같은 unique 키에 몰리면 진 쪽은 중복키(DataIntegrityViolationException) 뿐 아니라
     * 락 경합/데드락(다른 DataAccessException), 커밋 단계 실패(TransactionSystemException) 등 다양한 예외로 실패할 수 있다.
     * 예외 타입을 일일이 가리기 어려우므로 넓게 잡되, "잡은 뒤 방이 실제로 존재하면 경합으로 보고 반환,
     * 끝까지 없으면 원래 예외를 다시 던진다"는 식으로 처리한다. (진짜 오류는 그대로 표출됨)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatRoomResponse createDirectRoom(Long currentUserId, ChatRoomCreateRequest request) {
        validateCurrentUserIsParticipant(
                currentUserId,
                request.getTeacherId(),
                request.getStudentId()
        );

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));

        validateTeacherAndStudentRole(teacher, student);

        String roomKey = createDirectRoomKey(
                request.getTeacherId(),
                request.getStudentId()
        );

        ChatService self = selfProvider.getObject();

        RuntimeException lastError = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            // 1) 이미 있으면 그대로 반환 (대부분의 동시 요청은 여기서 끝난다)
            ChatRoomResponse existing = self.findDirectRoomResponse(roomKey, currentUserId);
            if (existing != null) {
                return existing;
            }

            // 2) 없으면 생성 시도. 동시 요청과 경합하면(중복키/락/커밋 실패 등) 다음 회차에서 재조회한다.
            try {
                return self.createDirectRoomInNewTx(
                        roomKey,
                        request.getTeacherId(),
                        request.getStudentId(),
                        currentUserId
                );
            } catch (RuntimeException e) {
                lastError = e;
            }
        }

        // 재시도 후 마지막 확인: 경합 끝에 누군가 만들어 둔 방이 있으면 그걸 반환
        ChatRoomResponse finalCheck = self.findDirectRoomResponse(roomKey, currentUserId);
        if (finalCheck != null) {
            return finalCheck;
        }
        // 경합이 아니라 진짜 DB 오류면 그대로 던진다
        throw lastError;
    }

    /**
     * roomKey 로 기존 1:1 방을 조회해 응답으로 변환. 없으면 null.
     *
     * 별도 트랜잭션으로 실행되어, 동시성 경합 후 재조회 시 새 스냅샷에서 먼저 커밋된 방을 볼 수 있다.
     * (외부에서 직접 쓰지 말 것 — createDirectRoom 내부 동시성 처리용.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public ChatRoomResponse findDirectRoomResponse(String roomKey, Long currentUserId) {
        return chatRoomRepository.findByRoomKey(roomKey)
                .map(room -> toChatRoomResponse(room, currentUserId))
                .orElse(null);
    }

    /**
     * 1:1 방을 새로 만들어 응답으로 변환. room_key 가 이미 있으면 UNIQUE 제약 위반으로 예외가 난다(호출부에서 처리).
     *
     * 응답 변환(지연 로딩 포함)을 같은 트랜잭션 안에서 끝내, 엔티티가 detach 되어 LazyInitializationException 이 나는 것을 막는다.
     * (외부에서 직접 쓰지 말 것 — createDirectRoom 내부 동시성 처리용.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatRoomResponse createDirectRoomInNewTx(
            String roomKey,
            Long teacherId,
            Long studentId,
            Long currentUserId
    ) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));

        /*
         * 기존 ChatRoom 엔티티는 course를 받을 수 있는 구조다.
         * 하지만 지금 단계의 매칭 문의 채팅은 Course가 아직 없어도 되어야 하므로 null을 넣는다.
         * 나중에 Course 기반 채팅으로 확장할 때 이 부분에서 Course를 조회해서 넣으면 된다.
         */
        ChatRoom newRoom = ChatRoom.createDirectRoom(null, roomKey);

        // 1:1 문의 채팅이므로 참여자는 선생님 1명, 학생 1명만 넣는다.
        ChatRoomParticipant.createTeacher(newRoom, teacher);
        ChatRoomParticipant.createStudent(newRoom, student);

        ChatRoom saved = chatRoomRepository.save(newRoom);

        return toChatRoomResponse(saved, currentUserId);
    }

    /**
     * 내가 참여 중인 채팅방 목록 조회.
     *
     * 지금은 1:1 문의 채팅만 만들지만, 기존 구조가 participant 기반이므로
     * 나중에 단체 채팅을 붙여도 이 조회 방식은 크게 바뀌지 않는다.
     */
    public List<ChatRoomResponse> getMyChatRooms(Long currentUserId) {
        return chatRoomRepository.findMyChatRooms(currentUserId).stream()
                .map(chatRoom -> toChatRoomResponse(chatRoom, currentUserId))
                .toList();
    }

    /**
     * 메시지 목록 조회.
     *
     * cursor 기반 페이징을 사용한다.
     * 프론트에서는 처음 조회할 때 cursor 없이 호출하고,
     * 더 과거 메시지를 볼 때 nextCursor를 다시 보내면 된다.
     */
    public ChatMessagePageResponse getMessages(
            Long currentUserId,
            Long roomId,
            Long cursor,
            int size
    ) {
        validateParticipant(roomId, currentUserId);

        int safeSize = Math.max(1, Math.min(size, 100));
        int requestSize = safeSize + 1;

        List<ChatMessage> fetched = chatMessageRepository.findMessagesByCursor(
                roomId,
                cursor,
                PageRequest.of(0, requestSize)
        );

        boolean hasNext = fetched.size() > safeSize;

        List<ChatMessage> messages = hasNext
                ? fetched.subList(0, safeSize)
                : fetched;

        Long nextCursor = messages.isEmpty()
                ? null
                : messages.get(messages.size() - 1).getId();

        List<ChatMessageResponse> responses = messages.stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .map(this::toChatMessageResponse)
                .toList();

        return new ChatMessagePageResponse(
                responses,
                nextCursor,
                hasNext
        );
    }

    /**
     * WebSocket으로 들어온 메시지를 DB에 저장하고 응답 DTO로 변환한다.
     *
     * 처리 순서:
     * 1. 채팅방 참여자인지 확인
     * 2. 메시지 저장
     * 3. 이미지 메시지면 file_asset과 연결
     * 4. chat_room.last_message_id / last_message_at 갱신
     * 5. WebSocket Controller에서 구독자에게 브로드캐스트
     */
    @Transactional
    public ChatMessageResponse sendMessage(
            Long currentUserId,
            Long roomId,
            ChatMessageSendRequest request
    ) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!chatRoom.isActive()) {
            throw new IllegalArgumentException("종료된 채팅방에는 메시지를 보낼 수 없습니다.");
        }

        validateParticipant(roomId, currentUserId);

        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ChatMessage message;

        if (request.getMessageType() == ChatMessageType.TEXT) {
            validateTextMessage(request);
            message = ChatMessage.createText(chatRoom, sender, request.getContent());
        } else if (request.getMessageType() == ChatMessageType.IMAGE) {
            validateImageMessage(request);
            message = ChatMessage.createImage(chatRoom, sender, request.getContent());
        } else {
            throw new IllegalArgumentException("지원하지 않는 메시지 타입입니다.");
        }

        ChatMessage savedMessage = chatMessageRepository.save(message);

        if (request.getMessageType() == ChatMessageType.IMAGE) {
            attachFiles(savedMessage, currentUserId, request.getFileIds());
        }

        chatRoom.updateLastMessage(savedMessage);

        return toChatMessageResponse(savedMessage);
    }

    /**
     * 읽음 처리.
     *
     * 1:1 채팅에서도 chat_room_participant.last_read_message_id를 사용한다.
     * 이 구조를 유지하면 나중에 단체 채팅으로 확장할 때도 읽음 처리 방식을 갈아엎지 않아도 된다.
     */
    @Transactional
    public ChatReadResponse readUpTo(
            Long currentUserId,
            Long roomId,
            Long lastReadMessageId
    ) {
        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 참여자가 아닙니다."));

        ChatMessage message = chatMessageRepository.findById(lastReadMessageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));

        if (!message.getChatRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("해당 채팅방의 메시지가 아닙니다.");
        }

        participant.readUpTo(message);

        return new ChatReadResponse(
                roomId,
                currentUserId,
                message.getId(),
                participant.getLastReadAt()
        );
    }

    /**
     * 이미지 메시지와 file_asset을 연결한다.
     *
     * chat_message에는 image_url을 직접 저장하지 않는다.
     * 실제 파일 정보는 file_asset에 있고, chat_message_attachment가 둘을 연결한다.
     */
    private void attachFiles(
            ChatMessage savedMessage,
            Long currentUserId,
            List<Long> fileIds
    ) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new IllegalArgumentException("첨부할 파일이 없습니다.");
        }

        List<Long> distinctFileIds = fileIds.stream()
                .distinct()
                .toList();

        if (distinctFileIds.size() != fileIds.size()) {
            throw new IllegalArgumentException("중복된 파일 ID가 포함되어 있습니다.");
        }

        List<FileAsset> files = fileAssetRepository.findByIdIn(fileIds);

        if (files.size() != fileIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 파일이 포함되어 있습니다.");
        }

        Map<Long, FileAsset> fileMap = files.stream()
                .collect(Collectors.toMap(FileAsset::getId, Function.identity()));

        List<ChatMessageAttachment> attachments = new ArrayList<>();

        for (int i = 0; i < fileIds.size(); i++) {
            FileAsset fileAsset = fileMap.get(fileIds.get(i));

            if (!fileAsset.isUsable()) {
                throw new IllegalArgumentException("사용할 수 없는 파일입니다.");
            }

            if (!fileAsset.isImage()) {
                throw new IllegalArgumentException("이미지 파일만 첨부할 수 있습니다.");
            }

            if (!fileAsset.getUploader().getId().equals(currentUserId)) {
                throw new IllegalArgumentException("본인이 업로드한 파일만 첨부할 수 있습니다.");
            }

            attachments.add(ChatMessageAttachment.create(
                    savedMessage,
                    fileAsset,
                    i
            ));
        }

        // 첨부마다 save 를 호출하는 대신 한 번에 저장한다.
        chatMessageAttachmentRepository.saveAll(attachments);
    }

    private void validateTextMessage(ChatMessageSendRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("텍스트 메시지 내용은 필수입니다.");
        }
    }

    private void validateImageMessage(ChatMessageSendRequest request) {
        if (request.getFileIds() == null || request.getFileIds().isEmpty()) {
            throw new IllegalArgumentException("이미지 메시지는 파일 ID가 필요합니다.");
        }
    }

    private void validateParticipant(Long roomId, Long userId) {
        boolean exists = chatRoomParticipantRepository
                .existsByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, userId);

        if (!exists) {
            throw new IllegalArgumentException("채팅방 참여자가 아닙니다.");
        }
    }

    private void validateCurrentUserIsParticipant(
            Long currentUserId,
            Long teacherId,
            Long studentId
    ) {
        if (!currentUserId.equals(teacherId) && !currentUserId.equals(studentId)) {
            throw new IllegalArgumentException("채팅방 생성 권한이 없습니다.");
        }
    }

    private void validateTeacherAndStudentRole(User teacher, User student) {
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new IllegalArgumentException("teacherId에 해당하는 사용자가 선생님이 아닙니다.");
        }

        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("studentId에 해당하는 사용자가 학생이 아닙니다.");
        }
    }

    /**
     * 1:1 문의 채팅방 중복 생성 방지용 roomKey.
     *
     * 지금은 Course를 연결하지 않으므로 선생님/학생 조합만으로 하나의 방을 만든다.
     */
    private String createDirectRoomKey(Long teacherId, Long studentId) {
        return "DIRECT:T:%d:S:%d".formatted(teacherId, studentId);
    }

    private ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, Long currentUserId) {
        List<ChatRoomParticipant> participants =
                chatRoomParticipantRepository.findByChatRoomIdAndLeftAtIsNull(chatRoom.getId());

        List<ChatParticipantResponse> participantResponses = participants.stream()
                .map(participant -> new ChatParticipantResponse(
                        participant.getUser().getId(),
                        participant.getUser().getName(),
                        participant.getParticipantType()
                ))
                .toList();

        Long lastReadMessageId = chatRoomParticipantRepository
                .findByChatRoomIdAndUserIdAndLeftAtIsNull(chatRoom.getId(), currentUserId)
                .map(ChatRoomParticipant::getLastReadMessage)
                .map(ChatMessage::getId)
                .orElse(null);

        long unreadCount = chatMessageRepository.countUnreadMessages(
                chatRoom.getId(),
                currentUserId,
                lastReadMessageId
        );

        ChatMessageResponse lastMessageResponse = chatRoom.getLastMessage() == null
                ? null
                : toChatMessageResponse(chatRoom.getLastMessage());

        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getRoomType(),
                chatRoom.getCourse() == null ? null : chatRoom.getCourse().getId(),
                participantResponses,
                lastMessageResponse,
                chatRoom.getLastMessageAt(),
                unreadCount,
                chatRoom.getCreatedAt()
        );
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        List<ChatAttachmentResponse> attachments = message.getAttachments().stream()
                .map(attachment -> {
                    FileAsset file = attachment.getFileAsset();

                    return new ChatAttachmentResponse(
                            file.getId(),
                            file.getFileUrl(),
                            file.getThumbnailUrl(),
                            file.getOriginalFileName(),
                            file.getContentType(),
                            file.getFileSize(),
                            file.getWidth(),
                            file.getHeight(),
                            attachment.getSortOrder()
                    );
                })
                .toList();

        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getMessageType(),
                message.getContent(),
                attachments,
                message.isDeleted(),
                message.getSentAt(),
                message.getCreatedAt()
        );
    }
}
