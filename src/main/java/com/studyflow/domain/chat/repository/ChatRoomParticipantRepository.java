package com.studyflow.domain.chat.repository;

import com.studyflow.domain.chat.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {

    /**
     * 현재 사용자가 특정 채팅방에 참여 중인지 확인한다.
     *
     * leftAt이 null이면 아직 나가지 않은 참여자다.
     */
    boolean existsByChatRoomIdAndUserIdAndLeftAtIsNull(Long chatRoomId, Long userId);

    /**
     * 특정 채팅방의 특정 참여자 조회.
     */
    Optional<ChatRoomParticipant> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 특정 채팅방의 현재 참여자 조회.
     */
    Optional<ChatRoomParticipant> findByChatRoomIdAndUserIdAndLeftAtIsNull(Long chatRoomId, Long userId);

    /**
     * 특정 채팅방의 현재 참여자 목록 조회.
     *
     * 1:1 매칭 문의 채팅에서는 결과가 선생님 1명 + 학생 1명, 총 2명이어야 한다.
     */
    List<ChatRoomParticipant> findByChatRoomIdAndLeftAtIsNull(Long chatRoomId);
    /**
     * 특정 채팅방의 모든 참여자(나간 사람 포함) 목록 조회.
     */
    List<ChatRoomParticipant> findByChatRoomId(Long chatRoomId);
}
