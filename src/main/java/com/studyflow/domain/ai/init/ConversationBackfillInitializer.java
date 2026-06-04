package com.studyflow.domain.ai.init;

import com.studyflow.domain.ai.entity.AiQuestion;
import com.studyflow.domain.ai.entity.Conversation;
import com.studyflow.domain.ai.repository.AiQuestionRepository;
import com.studyflow.domain.ai.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 대화(Conversation) 도입 이전에 저장된 질문들을 각각 "1질문짜리 대화"로 편입한다.
 *
 * <p>대화 묶음 기능을 켜기 전의 기존 AI 질문들은 {@code conversation_id}가 비어 있어 사이드바
 * (대화 목록)에 나타나지 않는다. 앱 기동 시 이들을 각자 하나의 대화로 만들어 연결해, 기존 기록도
 * 대화 목록에서 보이도록 한다. 이미 대화에 편입된 질문은 건드리지 않으므로 멱등하다.</p>
 *
 * <p>대상이 수만 건이어도 안전하도록 <b>한 배치(BATCH_SIZE)씩, 배치마다 별도 트랜잭션</b>으로
 * 처리한다. 전체를 한 번에 로드하지 않아 OOM이 없고, 트랜잭션이 끝날 때마다 영속성 컨텍스트가
 * 정리된다. 처리된 행은 조건(conversation IS NULL)에서 빠지므로 항상 첫 페이지만 다시 읽는다.</p>
 */
@Slf4j
@Component
@Order(2) // 과목 시딩(Order 1) 이후
@RequiredArgsConstructor
public class ConversationBackfillInitializer implements ApplicationRunner {

    /** 한 트랜잭션에서 처리할 질문 수. */
    private static final int BATCH_SIZE = 200;

    private final AiQuestionRepository aiQuestionRepository;
    private final ConversationRepository conversationRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int total = 0;
        while (true) {
            // 배치마다 새 트랜잭션 — self-invocation 문제 없이 경계를 명확히 하기 위해 TransactionTemplate 사용
            Integer processed = transactionTemplate.execute(status -> backfillOneBatch());
            if (processed == null || processed == 0) {
                break;
            }
            total += processed;
        }
        if (total > 0) {
            log.info("[ConversationBackfillInitializer] 기존 질문 {}건을 대화로 편입했습니다.", total);
        }
    }

    /** 미편입 질문 한 페이지를 대화로 편입하고 처리 건수를 반환한다. (호출자 트랜잭션 안에서 실행) */
    private int backfillOneBatch() {
        Page<AiQuestion> orphans = aiQuestionRepository.findByConversationIsNull(PageRequest.of(0, BATCH_SIZE));
        for (AiQuestion question : orphans) {
            Conversation conversation = Conversation.createFromFirstQuestion(
                    question.getUser(), question.getSubject(), question.getQuestionText());
            conversationRepository.save(conversation);
            question.assignConversation(conversation);
            aiQuestionRepository.save(question);
        }
        return orphans.getNumberOfElements();
    }
}
