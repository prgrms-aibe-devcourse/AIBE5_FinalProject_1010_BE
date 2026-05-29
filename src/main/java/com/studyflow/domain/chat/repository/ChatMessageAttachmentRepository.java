package com.studyflow.domain.chat.repository;

import com.studyflow.domain.chat.entity.ChatMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageAttachmentRepository extends JpaRepository<ChatMessageAttachment, Long> {
}
