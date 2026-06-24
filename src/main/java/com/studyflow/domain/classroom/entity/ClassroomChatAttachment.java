package com.studyflow.domain.classroom.entity;

import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 강의실 채팅 메시지 첨부(이미지) — file_asset과 강의실 채팅 메시지를 연결한다.
 * 1:1 채팅의 chat_message_attachment와 동일한 구조(메시지엔 URL을 직접 저장하지 않고 file_asset 참조).
 */
@Getter
@Entity
@Table(
        name = "classroom_chat_attachment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_classroom_chat_attachment_chat_file",
                        columnNames = {"classroom_chat_id", "file_id"}
                )
        },
        indexes = {
                @Index(name = "idx_classroom_chat_attachment_chat", columnList = "classroom_chat_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassroomChatAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_chat_id", nullable = false)
    private ClassroomChat classroomChat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileAsset fileAsset;

    /** 한 메시지에 이미지 여러 장일 때 표시 순서 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public static ClassroomChatAttachment create(ClassroomChat chat, FileAsset fileAsset, Integer sortOrder) {
        ClassroomChatAttachment a = new ClassroomChatAttachment();
        a.classroomChat = chat;
        a.fileAsset = fileAsset;
        a.sortOrder = sortOrder == null ? 0 : sortOrder;
        chat.addAttachment(a);
        return a;
    }
}
