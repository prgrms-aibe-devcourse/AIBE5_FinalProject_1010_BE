package com.studyflow.domain.chat.entity;

//import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.chat.enums.ChatRoomStatus;
import com.studyflow.domain.chat.enums.ChatRoomType;
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
        name = "chat_room",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_room_key",
                        columnNames = "room_key"
                )
        },
        indexes = {
                @Index(name = "idx_chat_room_course", columnList = "course_id"),
                @Index(name = "idx_chat_room_type", columnList = "room_type"),
                @Index(name = "idx_chat_room_last_message_at", columnList = "last_message_at"),
                @Index(name = "idx_chat_room_last_message", columnList = "last_message_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 채팅방 타입.
     *
     * DIRECT: 선생님 1명 + 학생 1명
     * COURSE_GROUP: 선생님 1명 + 학생 여러 명
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 30)
    private ChatRoomType roomType;

    /**
     * 연결된 수업.
     *
     * 수업 기반 채팅이면 값 존재.
     * 일반 채팅까지 열어둘 경우 null 가능.
     */
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "course_id")
//    private Course course;

    /**
     * 채팅방 중복 생성 방지용 키.
     *
     * 예:
     * DIRECT:COURSE:10:T:3:S:8
     * COURSE_GROUP:COURSE:10
     */
    @Column(name = "room_key", nullable = false, length = 200)
    private String roomKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;

    /**
     * 마지막 메시지.
     *
     * last_message_preview 대신 실제 메시지를 참조한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private ChatMessage lastMessage;

    /**
     * 채팅방 목록 최신순 정렬용.
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 채팅방 참여자 목록.
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoomParticipant> participants = new ArrayList<>();

    /**
     * 채팅방 메시지 목록.
     *
     * 실제 조회는 Repository에서 페이징 조회 추천.
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    public static ChatRoom createDirectRoom(
            //Course course,
            String roomKey
    ) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.roomType = ChatRoomType.DIRECT;
        //chatRoom.course = course;
        chatRoom.roomKey = roomKey;
        chatRoom.status = ChatRoomStatus.ACTIVE;
        return chatRoom;
    }

    public static ChatRoom createCourseGroupRoom(
            //Course course,
            String roomKey
    ) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.roomType = ChatRoomType.COURSE_GROUP;
        //chatRoom.course = course;
        chatRoom.roomKey = roomKey;
        chatRoom.status = ChatRoomStatus.ACTIVE;
        return chatRoom;
    }

    public void addParticipant(ChatRoomParticipant participant) {
        this.participants.add(participant);
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
    }

    /**
     * 마지막 메시지 갱신.
     *
     * 메시지를 저장한 뒤 서비스 계층에서 호출하는 것을 추천.
     */
    public void updateLastMessage(ChatMessage message) {
        this.lastMessage = message;
        this.lastMessageAt = message.getSentAt();
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
    }

    public boolean isActive() {
        return this.status == ChatRoomStatus.ACTIVE;
    }

    public boolean isDirectRoom() {
        return this.roomType == ChatRoomType.DIRECT;
    }

    public boolean isCourseGroupRoom() {
        return this.roomType == ChatRoomType.COURSE_GROUP;
    }
}