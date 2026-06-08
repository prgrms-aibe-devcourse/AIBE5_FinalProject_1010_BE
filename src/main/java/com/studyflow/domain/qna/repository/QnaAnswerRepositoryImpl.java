package com.studyflow.domain.qna.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyflow.domain.qna.entity.QnaAnswer;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.studyflow.domain.qna.entity.QQnaAnswer.qnaAnswer;
import static com.studyflow.domain.qna.entity.QQnaQuestion.qnaQuestion;

@RequiredArgsConstructor
public class QnaAnswerRepositoryImpl implements QnaAnswerRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<QnaAnswer> findByQuestionIdWithAuthor(Long questionId) {
        return queryFactory
                .selectFrom(qnaAnswer)
                .join(qnaAnswer.author).fetchJoin()
                .where(qnaAnswer.question.id.eq(questionId))
                .orderBy(qnaAnswer.accepted.desc(), qnaAnswer.likeCount.desc(), qnaAnswer.createdAt.asc())
                .fetch();
    }

    @Override
    public Optional<QnaAnswer> findDetailById(Long id) {
        QnaAnswer result = queryFactory
                .selectFrom(qnaAnswer)
                .join(qnaAnswer.question, qnaQuestion).fetchJoin()
                .join(qnaQuestion.author).fetchJoin()
                .join(qnaAnswer.author).fetchJoin()
                .where(qnaAnswer.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<QuestionAnswerCount> countByQuestionIds(List<Long> questionIds) {
        return queryFactory
                .select(Projections.constructor(QuestionAnswerCount.class,
                        qnaAnswer.question.id, qnaAnswer.count()))
                .from(qnaAnswer)
                .where(qnaAnswer.question.id.in(questionIds))
                .groupBy(qnaAnswer.question.id)
                .fetch();
    }
}
