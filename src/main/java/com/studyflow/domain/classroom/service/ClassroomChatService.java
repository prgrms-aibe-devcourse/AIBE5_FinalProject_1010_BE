package com.studyflow.domain.classroom.service;

import com.studyflow.domain.chat.enums.ChatMessageType;
import com.studyflow.domain.classroom.dto.request.ClassroomChatSendRequest;
import com.studyflow.domain.classroom.dto.response.ClassroomChatLikeResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomChatResponse;
import com.studyflow.domain.classroom.entity.ClassroomChat;
import com.studyflow.domain.classroom.entity.ClassroomChatAttachment;
import com.studyflow.domain.classroom.entity.ClassroomChatLike;
import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.exception.ClassroomNotOpenException;
import com.studyflow.domain.classroom.exception.ClassroomSessionNotFoundException;
import com.studyflow.domain.classroom.repository.ClassroomChatLikeRepository;
import com.studyflow.domain.classroom.repository.ClassroomChatRepository;
import com.studyflow.domain.classroom.repository.ClassroomSessionRepository;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 강의실 채팅 서비스 (apidetail.md 23장).
 *
 * <p>1:1 채팅과 동일하게 TEXT/IMAGE 메시지를 지원한다. IMAGE는 업로드된 file_asset을
 * classroom_chat_attachment로 연결한다(메시지에 URL을 직접 저장하지 않음).
 * 멤버십 검증(담당 선생님·ACTIVE 수강생)은 {@link ClassroomService#verifyMemberAndIsHost}를 재사용한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ClassroomChatService {

    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomChatRepository chatRepository;
    private final ClassroomChatLikeRepository chatLikeRepository;
    private final ClassroomService classroomService;
    private final UserRepository userRepository;
    private final FileAssetRepository fileAssetRepository;

    /**
     * 강의실 채팅 전송 (23-2) — 수업 멤버만. 열린 세션에만 전송 가능.
     * TEXT: content 필수 / IMAGE: fileIds 필수(content는 캡션 또는 null).
     */
    @Transactional
    public ClassroomChatResponse sendMessage(Long sessionId, Long userId, ClassroomChatSendRequest request) {
        ChatMessageType type = request.messageType() == null ? ChatMessageType.TEXT : request.messageType();

        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        if (!session.isOpen()) {
            throw new ClassroomNotOpenException("종료된 강의실에는 메시지를 보낼 수 없습니다.");
        }
        classroomService.verifyMemberAndIsHost(session.getCourse(), userId);

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. (userId: " + userId + ")"));

        if (type == ChatMessageType.IMAGE) {
            // 빈 캡션은 null이 아니라 ""로 — 기존 classroom_chat.content가 NOT NULL이라(ddl-auto는 제약 미변경) null 저장 시 실패.
            String caption = (request.content() == null || request.content().isBlank()) ? "" : request.content().trim();
            ClassroomChat chat = ClassroomChat.of(session, sender, ChatMessageType.IMAGE, caption);
            attachFiles(chat, userId, request.fileIds());
            return ClassroomChatResponse.from(chatRepository.save(chat));
        }

        // TEXT
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("메시지 내용이 비어 있습니다.");
        }
        ClassroomChat chat = ClassroomChat.of(session, sender, ChatMessageType.TEXT, request.content().trim());
        return ClassroomChatResponse.from(chatRepository.save(chat));
    }

    /**
     * 이미지 메시지에 file_asset을 연결한다(1:1 채팅 attachFiles와 동일 검증).
     * 본인이 업로드한 사용 가능한 이미지만, 순서대로.
     */
    private void attachFiles(ClassroomChat chat, Long userId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new IllegalArgumentException("첨부할 이미지가 없습니다.");
        }
        if (fileIds.stream().distinct().count() != fileIds.size()) {
            throw new IllegalArgumentException("중복된 파일 ID가 포함되어 있습니다.");
        }
        List<FileAsset> files = fileAssetRepository.findByIdIn(fileIds);
        if (files.size() != fileIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 파일이 포함되어 있습니다.");
        }
        Map<Long, FileAsset> fileMap = files.stream()
                .collect(Collectors.toMap(FileAsset::getId, Function.identity()));
        for (int i = 0; i < fileIds.size(); i++) {
            FileAsset f = fileMap.get(fileIds.get(i));
            if (!f.isUsable()) throw new IllegalArgumentException("사용할 수 없는 파일입니다.");
            if (!f.isImage()) throw new IllegalArgumentException("이미지 파일만 첨부할 수 있습니다.");
            if (!f.getUploader().getId().equals(userId)) {
                throw new IllegalArgumentException("본인이 업로드한 파일만 첨부할 수 있습니다.");
            }
            ClassroomChatAttachment.create(chat, f, i); // chat.attachments에 추가(cascade로 저장됨)
        }
    }

    /**
     * 강의실 채팅 이력 조회 (23-1) — 수업 멤버만. 시간순.
     */
    @Transactional(readOnly = true)
    public List<ClassroomChatResponse> getChats(Long sessionId, Long userId) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        classroomService.verifyMemberAndIsHost(session.getCourse(), userId);

        // 좋아요 수/내가 좋아요 여부를 일괄 조회(N+1 회피)
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : chatLikeRepository.countLikesBySession(sessionId)) {
            countMap.put((Long) row[0], (Long) row[1]);
        }
        Set<Long> likedByMe = new HashSet<>(chatLikeRepository.findLikedChatIdsBySessionAndUser(sessionId, userId));

        return chatRepository.findBySessionIdWithSender(sessionId).stream()
                .map(c -> ClassroomChatResponse.from(c,
                        countMap.getOrDefault(c.getId(), 0L),
                        likedByMe.contains(c.getId())))
                .toList();
    }

    /**
     * 강의실 채팅 좋아요 토글 — 수업 멤버만. 이미 눌렀으면 취소, 아니면 추가. 변경 후 좋아요 수 반환.
     */
    @Transactional
    public ClassroomChatLikeResponse toggleLike(Long sessionId, Long userId, Long chatId) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        classroomService.verifyMemberAndIsHost(session.getCourse(), userId);

        ClassroomChat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다. (chatId: " + chatId + ")"));
        if (!chat.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("이 강의실의 메시지가 아닙니다.");
        }

        if (chatLikeRepository.existsByClassroomChatIdAndUserId(chatId, userId)) {
            chatLikeRepository.deleteByClassroomChatIdAndUserId(chatId, userId);
        } else {
            chatLikeRepository.save(ClassroomChatLike.of(chat, userRepository.getReferenceById(userId)));
        }
        return new ClassroomChatLikeResponse(chatId, chatLikeRepository.countByClassroomChatId(chatId));
    }
}
