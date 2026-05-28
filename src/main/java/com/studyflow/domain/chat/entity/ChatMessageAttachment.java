package com.studyflow.domain.chat.entity;

import com.studyflow.domain.file.entity.FileAsset;

import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_message_attachment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_message_attachment_message_file",
                        columnNames = {"chat_message_id", "file_id"}
                )
        },
        indexes = {
                @Index(name = "idx_chat_message_attachment_message", columnList = "chat_message_id"),
                @Index(name = "idx_chat_message_attachment_file", columnList = "file_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 첨부파일이 연결된 메시지.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    /**
     * 실제 파일 메타데이터.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileAsset fileAsset;

    /**
     * 한 메시지에 이미지 여러 장이 있을 때 표시 순서.
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public static ChatMessageAttachment create(
            ChatMessage chatMessage,
            FileAsset fileAsset,
            Integer sortOrder
    ) {
        ChatMessageAttachment attachment = new ChatMessageAttachment();
        attachment.chatMessage = chatMessage;
        attachment.fileAsset = fileAsset;
        attachment.sortOrder = sortOrder == null ? 0 : sortOrder;

        chatMessage.addAttachment(attachment);

        return attachment;
    }
}