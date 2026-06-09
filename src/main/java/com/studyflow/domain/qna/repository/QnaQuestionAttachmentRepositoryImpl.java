package com.studyflow.domain.qna.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyflow.domain.qna.entity.QnaQuestionAttachment;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.studyflow.domain.qna.entity.QQnaQuestionAttachment.qnaQuestionAttachment;

@RequiredArgsConstructor
public class QnaQuestionAttachmentRepositoryImpl implements QnaQuestionAttachmentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<QnaQuestionAttachment> findByQuestionIdWithFile(Long questionId) {
        return queryFactory
                .selectFrom(qnaQuestionAttachment)
                .join(qnaQuestionAttachment.fileAsset).fetchJoin()
                .where(qnaQuestionAttachment.question.id.eq(questionId))
                .orderBy(qnaQuestionAttachment.sortOrder.asc())
                .fetch();
    }

    @Override
    public List<QnaQuestionAttachment> findFirstImagesByQuestionIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .selectFrom(qnaQuestionAttachment)
                .join(qnaQuestionAttachment.fileAsset).fetchJoin()
                .where(qnaQuestionAttachment.question.id.in(questionIds)
                        .and(qnaQuestionAttachment.sortOrder.eq(0)))
                .fetch();
    }
}
