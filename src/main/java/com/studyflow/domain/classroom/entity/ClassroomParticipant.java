package com.studyflow.domain.classroom.entity;

import com.studyflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 강의실 참가자 — 한 세션에 입장한 사용자 1명당 1행.
 *
 * <p>(session, user) 조합은 유일하다. 재입장 시 기존 행을 재사용한다.
 * 권한 플래그(canDraw 등)는 선생님이 참가자별로 변경할 수 있다.</p>
 */
@Entity
@Table(
        name = "classroom_participant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_classroom_participant_session_user",
                columnNames = {"classroom_session_id", "user_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassroomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_session_id", nullable = false)
    private ClassroomSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 화이트보드 판서 권한 */
    @Column(nullable = false)
    private boolean canDraw = false;

    /** 화면 공유 권한 */
    @Column(nullable = false)
    private boolean canShareScreen = false;

    /** 채팅 권한 */
    @Column(nullable = false)
    private boolean canChat = true;

    /**
     * 미디어 송출(카메라/마이크/화면) 권한 — LiveKit canPublish로 매핑.
     *
     * <p>한 방 다수 인원(예: 100명) 대비 "송출 게이팅": 기본적으로 선생님만 송출하고
     * 학생은 구독(시청)만 한다. 선생님이 발표시키려는 학생에게만 true로 부여한다.</p>
     */
    @Column(nullable = false)
    private boolean canPublish = false;

    /** 카메라 on/off 상태 (Lombok getter: isVideoOn()) */
    @Column(nullable = false)
    private boolean videoOn = false;

    /** 마이크 on/off 상태 (Lombok getter: isAudioOn()) */
    @Column(nullable = false)
    private boolean audioOn = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * 참가자 생성.
     *
     * @param host true면 선생님(개설자) — 판서·화면공유 권한을 기본 부여한다.
     */
    public static ClassroomParticipant join(ClassroomSession session, User user, boolean host) {
        ClassroomParticipant p = new ClassroomParticipant();
        p.session = session;
        p.user = user;
        p.canChat = true;
        p.canDraw = host;
        p.canShareScreen = host;
        p.canPublish = host;   // 선생님은 기본 송출 가능, 학생은 시청 전용
        p.videoOn = false;
        p.audioOn = false;
        return p;
    }

    /** 선생님이 참가자 권한을 변경 (canPublish=송출 게이팅 포함) */
    public void updatePermissions(boolean canDraw, boolean canShareScreen, boolean canChat, boolean canPublish) {
        this.canDraw = canDraw;
        this.canShareScreen = canShareScreen;
        this.canChat = canChat;
        this.canPublish = canPublish;
    }
}
