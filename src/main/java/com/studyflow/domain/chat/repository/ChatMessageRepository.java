package com.studyflow.domain.chat.repository;

import com.studyflow.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 채팅 메시지 커서 페이징 조회.
     *
     * cursor가 null이면 최신 메시지부터 조회한다.
     * cursor가 있으면 cursor보다 id가 작은, 즉 더 오래된 메시지를 조회한다.
     */
    @Query("""
        select m
        from ChatMessage m
        join fetch m.sender s
        where m.chatRoom.id = :roomId
          and (:cursor is null or m.id < :cursor)
        order by m.id desc
    """)
    List<ChatMessage> findMessagesByCursor(
            @Param("roomId") Long roomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    /**
     * 안 읽은 메시지 수를 계산한다.
     *
     * 내가 보낸 메시지는 unreadCount에서 제외한다.
     * lastReadMessageId가 null이면 상대방이 보낸 모든 메시지를 안 읽은 메시지로 본다.
     */
    @Query("""
        select count(m)
        from ChatMessage m
        where m.chatRoom.id = :roomId
          and m.deletedAt is null
          and m.sender.id <> :userId
          and (:lastReadMessageId is null or m.id > :lastReadMessageId)
    """)
    long countUnreadMessages(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );
}
