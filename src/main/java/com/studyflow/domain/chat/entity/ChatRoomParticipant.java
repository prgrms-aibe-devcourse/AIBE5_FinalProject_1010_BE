package com.studyflow.domain.chat.entity;

import com.studyflow.domain.chat.enums.ChatParticipantType;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_room_participant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_participant_room_user",
                        columnNames = {"chat_room_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_chat_participant_user", columnList = "user_id"),
                @Index(name = "idx_chat_participant_room", columnList = "chat_room_id"),
                @Index(name = "idx_chat_participant_type", columnList = "participant_type"),
                @Index(name = "idx_chat_participant_last_read_message", columnList = "last_read_message_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 참여 중인 채팅방.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 참여 사용자.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 선생님 / 학생 구분.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20)
    private ChatParticipantType participantType;

    /**
     * 채팅방 참여 시각.
     */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * 채팅방 나간 시각.
     *
     * null이면 현재 참여 중.
     */
    @Column(name = "left_at")
    private LocalDateTime leftAt;

    /**
     * 이 사용자가 마지막으로 읽은 메시지.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ChatMessage lastReadMessage;

    /**
     * 마지막 읽음 처리 시각.
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    public static ChatRoomParticipant createTeacher(
            ChatRoom chatRoom,
            User teacher
    ) {
        return create(chatRoom, teacher, ChatParticipantType.TEACHER);
    }

    public static ChatRoomParticipant createStudent(
            ChatRoom chatRoom,
            User student
    ) {
        return create(chatRoom, student, ChatParticipantType.STUDENT);
    }

    private static ChatRoomParticipant create(
            ChatRoom chatRoom,
            User user,
            ChatParticipantType participantType
    ) {
        ChatRoomParticipant participant = new ChatRoomParticipant();
        participant.chatRoom = chatRoom;
        participant.user = user;
        participant.participantType = participantType;
        participant.joinedAt = LocalDateTime.now();

        chatRoom.addParticipant(participant);

        return participant;
    }

    public void readUpTo(ChatMessage message) {
        this.lastReadMessage = message;
        this.lastReadAt = LocalDateTime.now();
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.leftAt == null;
    }

    public boolean isTeacher() {
        return this.participantType == ChatParticipantType.TEACHER;
    }

    public boolean isStudent() {
        return this.participantType == ChatParticipantType.STUDENT;
    }
}