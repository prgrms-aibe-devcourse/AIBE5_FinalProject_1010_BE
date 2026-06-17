package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomChatLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassroomChatLikeRepository extends JpaRepository<ClassroomChatLike, Long> {

    boolean existsByClassroomChatIdAndUserId(Long classroomChatId, Long userId);

    void deleteByClassroomChatIdAndUserId(Long classroomChatId, Long userId);

    long countByClassroomChatId(Long classroomChatId);

    /** 세션 내 사용자가 좋아요한 채팅 id 목록(이력 조회 시 likedByMe 판정용). */
    @Query("select l.classroomChat.id from ClassroomChatLike l " +
            "where l.classroomChat.session.id = :sessionId and l.user.id = :userId")
    List<Long> findLikedChatIdsBySessionAndUser(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    /** 세션 내 채팅별 좋아요 수([chatId, count]) — 이력 조회 시 일괄 집계. */
    @Query("select l.classroomChat.id, count(l) from ClassroomChatLike l " +
            "where l.classroomChat.session.id = :sessionId group by l.classroomChat.id")
    List<Object[]> countLikesBySession(@Param("sessionId") Long sessionId);
}
