package com.studyflow.domain.chat.entity;

import com.studyflow.domain.chat.enums.ChatMessageType;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "chat_message",
        indexes = {
                @Index(name = "idx_chat_message_room_sent_at", columnList = "chat_room_id, sent_at"),
                @Index(name = "idx_chat_message_sender", columnList = "sender_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 메시지가 속한 채팅방.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 메시지를 보낸 사용자.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * 메시지 타입.
     *
     * TEXT / IMAGE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType messageType;

    /**
     * 메시지 내용.
     *
     * TEXT: 본문
     * IMAGE: 캡션 또는 null
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 메시지 전송 시각.
     */
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /**
     * 메시지 삭제 시각.
     *
     * 실제 row 삭제 대신 soft delete 용도.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 메시지 첨부파일 목록.
     */
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessageAttachment> attachments = new ArrayList<>();

    public static ChatMessage createText(
            ChatRoom chatRoom,
            User sender,
            String content
    ) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = sender;
        message.messageType = ChatMessageType.TEXT;
        message.content = content;
        message.sentAt = LocalDateTime.now();

        chatRoom.addMessage(message);

        return message;
    }

    public static ChatMessage createImage(
            ChatRoom chatRoom,
            User sender,
            String caption
    ) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = sender;
        message.messageType = ChatMessageType.IMAGE;
        message.content = caption;
        message.sentAt = LocalDateTime.now();

        chatRoom.addMessage(message);

        return message;
    }

    public void addAttachment(ChatMessageAttachment attachment) {
        this.attachments.add(attachment);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isTextMessage() {
        return this.messageType == ChatMessageType.TEXT;
    }

    public boolean isImageMessage() {
        return this.messageType == ChatMessageType.IMAGE;
    }

    public boolean hasAttachment() {
        return !this.attachments.isEmpty();
    }
}