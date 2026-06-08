package com.studyflow.domain.qna.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.studyflow.domain.qna.entity.QQnaAnswerLike.qnaAnswerLike;

@RequiredArgsConstructor
public class QnaAnswerLikeRepositoryImpl implements QnaAnswerLikeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findLikedAnswerIds(Long userId, List<Long> answerIds) {
        return queryFactory
                .select(qnaAnswerLike.answer.id)
                .from(qnaAnswerLike)
                .where(qnaAnswerLike.user.id.eq(userId),
                        qnaAnswerLike.answer.id.in(answerIds))
                .fetch();
    }
}
