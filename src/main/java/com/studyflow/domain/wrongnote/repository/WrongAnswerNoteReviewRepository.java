package com.studyflow.domain.wrongnote.repository;

import com.studyflow.domain.wrongnote.entity.WrongAnswerNoteReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WrongAnswerNoteReviewRepository extends JpaRepository<WrongAnswerNoteReview, Long> {

    Page<WrongAnswerNoteReview> findByNoteIdAndNoteOwnerIdOrderByReviewedAtDesc(Long noteId, Long ownerId, Pageable pageable);
}
