package com.studyflow.domain.classroom.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 강의실 채팅 메시지 — 한 강의실 세션(ClassroomSession) 안에서 오간 텍스트 채팅.
 *
 * <p>apidetail.md 23장. 1:1 채팅(chat_message)과 별개로, "방=강의실 세션" 단위로 브로드캐스트된다.
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

    /** 메시지 본문 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private ClassroomChat(ClassroomSession session, User sender, String content) {
        this.session = session;
        this.sender = sender;
        this.content = content;
    }

    public static ClassroomChat of(ClassroomSession session, User sender, String content) {
        return new ClassroomChat(session, sender, content);
    }
}
