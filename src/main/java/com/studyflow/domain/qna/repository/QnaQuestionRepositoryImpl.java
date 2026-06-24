package com.studyflow.domain.qna.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyflow.domain.qna.entity.QnaQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.studyflow.domain.qna.entity.QQnaAnswer.qnaAnswer;
import static com.studyflow.domain.qna.entity.QQnaQuestion.qnaQuestion;

@RequiredArgsConstructor
public class QnaQuestionRepositoryImpl implements QnaQuestionRepositoryCustom {

    // 정렬 허용 필드 화이트리스트 — 존재하지 않는 sort 파라미터로 인한 런타임 오류를 막는다.
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "viewCount", "resolved", "id");

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<QnaQuestion> findFiltered(Long subjectId, String keyword, Boolean resolved, Pageable pageable) {
        BooleanExpression[] conditions = {
                subjectEq(subjectId),
                resolvedEq(resolved),
                keywordContains(keyword)
        };

        List<QnaQuestion> content = queryFactory
                .selectFrom(qnaQuestion)
                .join(qnaQuestion.subject).fetchJoin()
                .join(qnaQuestion.author).fetchJoin()
                .where(conditions)
                .orderBy(toOrders(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qnaQuestion.count())
                .from(qnaQuestion)
                .where(conditions)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Page<QnaQuestion> findFilteredOrderByAnswerCount(Long subjectId, String keyword, Boolean resolved, Pageable pageable) {
        BooleanExpression[] conditions = {
                subjectEq(subjectId),
                resolvedEq(resolved),
                keywordContains(keyword)
        };

        // 1) 답변 많은순으로 정렬된 질문 id만 페이지 단위로 조회 (left join + group by + count)
        List<Long> ids = queryFactory
                .select(qnaQuestion.id)
                .from(qnaQuestion)
                .leftJoin(qnaAnswer).on(qnaAnswer.question.id.eq(qnaQuestion.id))
                .where(conditions)
                .groupBy(qnaQuestion.id)
                .orderBy(qnaAnswer.count().desc(), qnaQuestion.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qnaQuestion.count())
                .from(qnaQuestion)
                .where(conditions)
                .fetchOne();

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total == null ? 0L : total);
        }

        // 2) 해당 id들을 subject·author fetch join으로 가져온 뒤 id 순서대로 재정렬
        List<QnaQuestion> fetched = queryFactory
                .selectFrom(qnaQuestion)
                .join(qnaQuestion.subject).fetchJoin()
                .join(qnaQuestion.author).fetchJoin()
                .where(qnaQuestion.id.in(ids))
                .fetch();
        Map<Long, QnaQuestion> byId = fetched.stream().collect(Collectors.toMap(QnaQuestion::getId, q -> q));
        List<QnaQuestion> content = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Optional<QnaQuestion> findDetailById(Long id) {
        QnaQuestion result = queryFactory
                .selectFrom(qnaQuestion)
                .join(qnaQuestion.subject).fetchJoin()
                .join(qnaQuestion.author).fetchJoin()
                .where(qnaQuestion.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    private BooleanExpression subjectEq(Long subjectId) {
        return subjectId == null ? null : qnaQuestion.subject.id.eq(subjectId);
    }

    private BooleanExpression resolvedEq(Boolean resolved) {
        return resolved == null ? null : qnaQuestion.resolved.eq(resolved);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null) {
            return null;
        }
        // 제목 또는 내용에 검색어 포함 (LIKE %keyword%)
        return qnaQuestion.title.contains(keyword).or(qnaQuestion.content.contains(keyword));
    }

    /**
     * Pageable의 정렬 조건을 OrderSpecifier로 변환한다. 정렬이 없으면 최신순(createdAt DESC)을 기본 적용한다.
     * 엔티티 속성명을 그대로 정렬 키로 사용한다(기존 Spring Data + Pageable 동작과 동일).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private OrderSpecifier<?>[] toOrders(Sort sort) {
        if (sort.isEmpty()) {
            return new OrderSpecifier<?>[]{qnaQuestion.createdAt.desc()};
        }
        PathBuilder<QnaQuestion> path = new PathBuilder<>(QnaQuestion.class, qnaQuestion.getMetadata().getName());
        List<OrderSpecifier> orders = new ArrayList<>();
        for (Sort.Order o : sort) {
            // 화이트리스트에 없는 필드는 무시 (잘못된 sort 파라미터로 QueryDSL 런타임 오류 방지)
            if (!ALLOWED_SORT_FIELDS.contains(o.getProperty())) {
                continue;
            }
            orders.add(new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC,
                    path.get(o.getProperty())));
        }
        // 유효한 정렬이 하나도 없으면 최신순 기본 적용
        if (orders.isEmpty()) {
            return new OrderSpecifier<?>[]{qnaQuestion.createdAt.desc()};
        }
        return orders.toArray(new OrderSpecifier[0]);
    }
}
