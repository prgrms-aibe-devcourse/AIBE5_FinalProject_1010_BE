package com.studyflow.domain.qna.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyflow.domain.qna.entity.QnaAnswerAttachment;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.studyflow.domain.qna.entity.QQnaAnswerAttachment.qnaAnswerAttachment;

@RequiredArgsConstructor
public class QnaAnswerAttachmentRepositoryImpl implements QnaAnswerAttachmentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<QnaAnswerAttachment> findByAnswerIdsWithFile(List<Long> answerIds) {
        return queryFactory
                .selectFrom(qnaAnswerAttachment)
                .join(qnaAnswerAttachment.fileAsset).fetchJoin()
                .where(qnaAnswerAttachment.answer.id.in(answerIds))
                .orderBy(qnaAnswerAttachment.sortOrder.asc())
                .fetch();
    }
}
