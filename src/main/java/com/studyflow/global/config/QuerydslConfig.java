package com.studyflow.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 설정.
 *
 * <p>커스텀 레포지토리 구현({@code *RepositoryImpl})에서 주입받아 사용할
 * {@link JPAQueryFactory} 빈을 등록한다. 프로젝트 최초의 QueryDSL 사용 지점이다.</p>
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
