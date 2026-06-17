package com.studyflow.domain.classroom.entity;

import com.studyflow.domain.chat.enums.ChatMessageType;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 강의실 채팅 메시지 — 한 강의실 세션(ClassroomSession) 안에서 오간 채팅(텍스트/이미지).
 *
 * <p>apidetail.md 23장. 1:1 채팅(chat_message)과 별개로, "방=강의실 세션" 단위로 브로드캐스트된다.
 * 1:1 채팅처럼 messageType(TEXT/IMAGE) + 이미지 첨부(classroom_chat_attachment)를 지원한다.
 * createdAt은 BaseTimeEntity가 채운다.</p>
 */
@Entity
@Table(
        name = "classroom_chat",
        indexes = {
                @Index(name = "idx_classroom_chat_session_created", columnList = "session_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassroomChat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 메시지가 속한 강의실 세션 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassroomSession session;

    /** 메시지를 보낸 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /** 메시지 종류 — TEXT(본문) / IMAGE(이미지 첨부, content는 캡션 또는 null). null(기존 행)은 TEXT로 간주 */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ChatMessageType messageType = ChatMessageType.TEXT;

    /** 메시지 본문(IMAGE면 캡션, 캡션 없으면 ""). 항상 non-null로 저장해 기존 NOT NULL 제약과 일치(마이그레이션 불필요). */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 이미지 첨부(여러 장) */
    @OneToMany(mappedBy = "classroomChat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassroomChatAttachment> attachments = new ArrayList<>();

    private ClassroomChat(ClassroomSession session, User sender, ChatMessageType messageType, String content) {
        this.session = session;
        this.sender = sender;
        this.messageType = messageType;
        this.content = content;
    }

    public static ClassroomChat of(ClassroomSession session, User sender, ChatMessageType messageType, String content) {
        return new ClassroomChat(session, sender, messageType, content);
    }

    public void addAttachment(ClassroomChatAttachment attachment) {
        this.attachments.add(attachment);
    }
}
