package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswerLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QnaAnswerLikeRepository extends JpaRepository<QnaAnswerLike, Long>, QnaAnswerLikeRepositoryCustom {

    Optional<QnaAnswerLike> findByAnswerIdAndUserId(Long answerId, Long userId);

    // 테스트 검증 전용 — 프로덕션 서비스에서 직접 호출 금지
    long countByAnswerId(Long answerId);

    // findLikedAnswerIds는 QnaAnswerLikeRepositoryCustom(QueryDSL)에 정의되어 있다.
}
