package com.studyflow.domain.notification.repository;

import com.studyflow.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 내 알림 목록 — 정렬은 컨트롤러 @PageableDefault(sort="createdAt", direction=DESC)에 위임
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    // 안 읽은 알림 수 (벨 뱃지)
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // 전체 읽음 처리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllReadByRecipientId(@Param("recipientId") Long recipientId);

    // 전체 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :recipientId")
    int deleteAllByRecipientId(@Param("recipientId") Long recipientId);
}
