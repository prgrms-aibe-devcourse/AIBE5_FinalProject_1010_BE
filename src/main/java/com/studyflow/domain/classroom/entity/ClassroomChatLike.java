package com.studyflow.domain.classroom.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 강의실 채팅 메시지 좋아요 — (메시지, 사용자) 유일. 한 사용자는 한 메시지에 1개만.
 */
@Getter
@Entity
@Table(
        name = "classroom_chat_like",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_classroom_chat_like", columnNames = {"classroom_chat_id", "user_id"})
        },
        indexes = { @Index(name = "idx_classroom_chat_like_chat", columnList = "classroom_chat_id") }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassroomChatLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_chat_id", nullable = false)
    private ClassroomChat classroomChat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static ClassroomChatLike of(ClassroomChat chat, User user) {
        ClassroomChatLike l = new ClassroomChatLike();
        l.classroomChat = chat;
        l.user = user;
        return l;
    }
}
