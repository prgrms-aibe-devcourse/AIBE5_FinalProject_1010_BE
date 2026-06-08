package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswerLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QnaAnswerLikeRepository extends JpaRepository<QnaAnswerLike, Long>, QnaAnswerLikeRepositoryCustom {

    boolean existsByAnswerIdAndUserId(Long answerId, Long userId);

    Optional<QnaAnswerLike> findByAnswerIdAndUserId(Long answerId, Long userId);

    // findLikedAnswerIds는 QnaAnswerLikeRepositoryCustom(QueryDSL)에 정의되어 있다.
}
