package com.studyflow.domain.chat.repository;

import com.studyflow.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * roomKey로 기존 채팅방을 찾는다.
     *
     * 1:1 매칭 문의 채팅에서는 같은 선생님/학생 조합으로 채팅방이 여러 개 생기면 안 된다.
     * 그래서 "DIRECT:T:{teacherId}:S:{studentId}" 형태의 roomKey를 unique 값처럼 사용한다.
     */
    Optional<ChatRoom> findByRoomKey(String roomKey);

    /**
     * 내가 참여 중인 채팅방 목록을 조회한다.
     *
     * 현재는 1:1 문의 채팅만 사용하지만, 기존 엔티티 구조는 chat_room_participant를 유지한다.
     * 따라서 teacher/student를 chat_room 컬럼에 직접 넣지 않고,
     * chat_room_participant에 들어간 참여자 기준으로 내 채팅방을 찾는다.
     */
    @Query("""
        select distinct cr
        from ChatRoom cr
        join cr.participants p
        left join fetch cr.course c
        left join fetch cr.lastMessage lm
        where p.user.id = :userId
          and p.leftAt is null
        order by
          case when cr.lastMessageAt is null then 1 else 0 end,
          cr.lastMessageAt desc
    """)
    List<ChatRoom> findMyChatRooms(@Param("userId") Long userId);
}
