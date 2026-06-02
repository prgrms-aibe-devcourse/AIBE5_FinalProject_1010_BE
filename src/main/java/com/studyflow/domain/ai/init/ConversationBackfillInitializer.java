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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 대화(Conversation) 도입 이전에 저장된 질문들을 각각 "1질문짜리 대화"로 편입한다.
 *
 * <p>대화 묶음 기능을 켜기 전의 기존 AI 질문들은 {@code conversation_id}가 비어 있어 사이드바
 * (대화 목록)에 나타나지 않는다. 앱 기동 시 이들을 각자 하나의 대화로 만들어 연결해, 기존 기록도
 * 대화 목록에서 보이도록 한다. 이미 대화에 편입된 질문은 건드리지 않으므로 멱등하다.</p>
 */
@Slf4j
@Component
@Order(2) // 과목 시딩(Order 1) 이후
@RequiredArgsConstructor
public class ConversationBackfillInitializer implements ApplicationRunner {

    private final AiQuestionRepository aiQuestionRepository;
    private final ConversationRepository conversationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<AiQuestion> orphans = aiQuestionRepository.findByConversationIsNull();
        if (orphans.isEmpty()) {
            return;
        }
        for (AiQuestion question : orphans) {
            Conversation conversation = Conversation.createFromFirstQuestion(
                    question.getUser(), question.getSubject(), question.getQuestionText());
            conversationRepository.save(conversation);
            question.assignConversation(conversation);
            aiQuestionRepository.save(question);
        }
        log.info("[ConversationBackfillInitializer] 기존 질문 {}건을 대화로 편입했습니다.", orphans.size());
    }
}
