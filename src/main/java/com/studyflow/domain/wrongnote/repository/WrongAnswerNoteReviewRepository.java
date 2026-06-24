package com.studyflow.domain.wrongnote.repository;

import com.studyflow.domain.wrongnote.entity.WrongAnswerNoteReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.studyflow.domain.wrongnote.enums.WrongAnswerReviewResult;

public interface WrongAnswerNoteReviewRepository extends JpaRepository<WrongAnswerNoteReview, Long> {

    Page<WrongAnswerNoteReview> findByNoteIdAndNoteOwnerIdAndResultNotOrderByReviewedAtDesc(
            Long noteId, Long ownerId, WrongAnswerReviewResult excludedResult, Pageable pageable);
}
