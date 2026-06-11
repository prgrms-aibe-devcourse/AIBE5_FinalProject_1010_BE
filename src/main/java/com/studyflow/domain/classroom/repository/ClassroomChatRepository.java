package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassroomChatRepository extends JpaRepository<ClassroomChat, Long> {

    /**
     * 세션의 전체 채팅을 시간순으로 조회한다(발신자 fetch join — 이름 표시 시 N+1 회피).
     */
    @Query("select c from ClassroomChat c join fetch c.sender " +
            "where c.session.id = :sessionId order by c.createdAt asc")
    List<ClassroomChat> findBySessionIdWithSender(@Param("sessionId") Long sessionId);
}
