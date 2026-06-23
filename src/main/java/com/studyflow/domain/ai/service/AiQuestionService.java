package com.studyflow.domain.ai.service;

import com.studyflow.domain.ai.client.OpenAiClient;
import com.studyflow.domain.ai.client.OpenAiImageClient;
import com.studyflow.domain.ai.dto.request.AiQuestionCreateRequest;
import com.studyflow.domain.ai.dto.response.AiQuestionHistoryResponse;
import com.studyflow.domain.ai.dto.response.AiQuestionResponse;
import com.studyflow.domain.ai.dto.response.ConversationDetailResponse;
import com.studyflow.domain.ai.dto.response.ConversationSummaryResponse;
import com.studyflow.domain.ai.entity.AiQuestion;
import com.studyflow.domain.ai.entity.AiQuestionAttachment;
import com.studyflow.domain.ai.entity.Conversation;
import com.studyflow.domain.ai.exception.AiQuestionNotFoundException;
import com.studyflow.domain.ai.exception.ConversationNotFoundException;
import com.studyflow.domain.ai.exception.SubjectNotFoundException;
import com.studyflow.domain.ai.repository.AiQuestionRepository;
import com.studyflow.domain.ai.repository.ConversationRepository;
import com.studyflow.domain.credit.CreditPolicy;
import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.credit.service.CreditService;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.file.service.FileService;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subscription.enums.SubscriptionType;
import com.studyflow.domain.subscription.service.SubscriptionService;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * AI 질문 도메인 서비스.
 *
 * <p>외부 AI(OpenAI) 호출과 DB 저장/조회를 조율한다.</p>
 * <ul>
 *   <li>질문 요청: 과목 검증 → OpenAI 호출 → 질문+답변 저장 → 응답</li>
 *   <li>기록 조회: 내 질문 목록을 최신순으로 반환</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private final AiQuestionRepository aiQuestionRepository;
    private final ConversationRepository conversationRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;
    private final SubscriptionService subscriptionService;
    private final FileAssetRepository fileAssetRepository;
    private final OpenAiClient openAiClient;
    private final OpenAiImageClient openAiImageClient;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    /**
     * "그림/이미지로 답변해줘"류 요청을 감지하는 키워드.
     *
     * <p>여기에 걸리고 <b>첨부 이미지가 없으면</b> 텍스트 답변 대신 이미지 생성으로 라우팅한다.
     * (첨부가 있으면 그 이미지에 대한 vision 질문이므로 생성하지 않는다.)</p>
     */
    private static final List<String> IMAGE_REQUEST_KEYWORDS = List.of(
            "그려줘", "그려 줘", "그려주세요", "그려 주세요", "그려줄래", "그려 줄래", "그려달", "그려 달",
            "그림으로", "사진으로", "이미지로", "그림 그려", "그림그려", "그림을 그", "그림 만들", "그림만들",
            "이미지 만들", "이미지만들", "이미지 생성", "이미지생성", "그림 생성", "그래프 그려", "도표로 그려",
            "일러스트", "draw"
    );

    /**
     * AI에게 질문하고 답변을 저장한다. (POST /api/v1/ai/questions)
     *
     * @param userId  인증된 사용자 id (@AuthenticationPrincipal)
     * @param request 질문 요청 (subjectId, questionText, questionImageUrl)
     * @return 저장된 질문 + AI 답변
     */
    /*
     * 의도적으로 메서드에 @Transactional을 두지 않는다.
     *
     * OpenAI 호출은 최대 수십 초가 걸리는 외부 I/O다. 이를 트랜잭션 안에서 호출하면 그동안
     * DB 커넥션을 점유해, 동시 요청이 쌓이면 커넥션 풀이 고갈되고 다른 API까지 멈출 수 있다.
     * 따라서 (1) 검증/조회와 (2) 저장만 각각 짧은 트랜잭션 경계(JpaRepository 메서드는 자체
     * 트랜잭션)에서 처리하고, OpenAI 호출은 그 사이의 트랜잭션 밖에서 수행한다.
     */
    public AiQuestionResponse ask(Long userId, AiQuestionCreateRequest request) {
        // 1) 사용자/과목 유효성 확인 (각 findById가 자체 트랜잭션)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));

        // 1-1) AI 질문 사용료(마일리지) 차감. 잔액 부족이면 InsufficientCreditException → 충전 유도.
        chargeAiQuestionIfNeeded(userId);

        // 2) 첨부 이미지(선택, 여러 장) 해석: fileId 목록을 FileAsset 목록으로 변환·검증한다.
        List<FileAsset> images = resolveImages(request.questionImageFileIds(), userId);

        // 3) 대화 결정: conversationId가 있으면 이어쓰기, 없으면 새 대화 생성.
        Conversation conversation = resolveConversation(user, subject, request.conversationId(), request.questionText());

        // 4) 이어지는 대화면 이전 질문·답변들을 맥락(history)으로 함께 보낸다.
        //    (이게 없으면 모델은 매번 첫 질문으로 받아 "방금 그 문제"류 후속 질문을 이해 못 한다)
        List<OpenAiClient.Turn> history = loadHistory(request.conversationId());

        // 5) 답변 생성 — 트랜잭션 밖에서 수행(외부 I/O로 인한 커넥션 점유 방지).
        //    요청 성격에 따라 셋 중 하나로 라우팅한다.
        String answerText = generateAnswer(userId, subject, request.questionText(), images, history);

        // 6) 질문 + 답변 + 첨부를 대화에 저장하고 응답으로 변환한다.
        AiQuestion saved = persist(user, subject, conversation, request.questionText(), answerText, images);
        return AiQuestionResponse.from(saved);
    }

    /**
     * AI 답변을 토큰 단위로 스트리밍하고, 끝나면 질문+전체답변을 저장한다.
     * (POST /api/v1/ai/questions/stream, Server-Sent Events)
     *
     * <p>이벤트 규약:</p>
     * <ul>
     *   <li>기본(message) 이벤트: 답변 조각(델타) 텍스트를 순서대로 방출</li>
     *   <li>{@code done} 이벤트: 스트림 종료 후 저장된 기록을 {@link AiQuestionResponse} JSON으로 1회 방출</li>
     *   <li>{@code error} 이벤트: 생성 중 오류 발생 시 사유 메시지 방출</li>
     * </ul>
     *
     * <p>검증(사용자/과목/첨부)은 스트림을 만들기 전에 동기로 수행하므로, 잘못된 요청은
     * 스트림이 아니라 일반 HTTP 에러(404/400 등)로 즉시 응답된다.</p>
     *
     * @param userId  인증된 사용자 id
     * @param request 질문 요청
     * @return SSE 이벤트 스트림
     */
    private void chargeAiQuestionIfNeeded(Long userId) {
        if (subscriptionService.hasActiveSubscription(userId, SubscriptionType.AI_QUESTION)) {
            return;
        }
        creditService.deduct(userId, CreditPolicy.AI_QUESTION_COST, CreditReason.AI_QUESTION, null);
    }
    public Flux<ServerSentEvent<String>> askStream(Long userId, AiQuestionCreateRequest request) {
        // 1) 스트림 시작 전에 동기로 검증한다(여기서 던진 예외는 GlobalExceptionHandler가 처리).
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));
        // AI 질문 사용료(마일리지) 차감 — 스트림 시작 전에 동기로(잔액 부족이면 여기서 막힘).
        chargeAiQuestionIfNeeded(userId);
        // 첨부 이미지는 소유/유효성만 검증해 저장 시 연결한다.
        // (1단계: 동기 ask()와 동일하게 OpenAI 호출에는 텍스트만 보내고, 이미지는 vision으로
        //  전달하지 않는다. 2단계에서 images의 URL들을 vision 입력으로 확장 예정.)
        List<FileAsset> images = resolveImages(request.questionImageFileIds(), userId);
        // 대화 결정(이어쓰기/새 대화)도 스트림 시작 전에 동기로 끝낸다.
        Conversation conversation = resolveConversation(user, subject, request.conversationId(), request.questionText());

        // done(정상 종료)과 doOnCancel(클라이언트 끊김)이 모두 저장을 시도할 수 있으므로,
        // 저장이 정확히 한 번만 일어나도록 결과를 담아 가드한다.
        AtomicReference<AiQuestion> savedRef = new AtomicReference<>();

        // 1-1) "그림으로 그려줘"류 + 첨부 없음 → 이미지 생성으로 라우팅한다.
        //      이미지 생성은 토큰 스트리밍이 아니므로, 생성/저장 후 답변(마크다운 이미지)을
        //      한 번에 흘려보내고 done 이벤트로 마무리한다.
        if (images.isEmpty() && isImageGenerationRequest(request.questionText())) {
            return Mono.fromCallable(() -> {
                        byte[] png = openAiImageClient.generatePng(request.questionText());
                        FileAsset generated = fileService.saveGeneratedImage(userId, png);
                        String answerText = buildImageAnswerText(generated.getFileUrl());
                        return persistOnce(savedRef, user, subject, conversation, request.questionText(), answerText, images);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(saved -> Flux.just(
                            // 답변(마크다운 이미지) 전체를 한 토큰으로 보내 말풍선을 만들고,
                            ServerSentEvent.<String>builder(saved.getAnswerText()).build(),
                            // 저장된 기록을 done으로 1회 방출한다.
                            ServerSentEvent.<String>builder(toJson(AiQuestionResponse.from(saved)))
                                    .event("done")
                                    .build()
                    ))
                    .onErrorResume(e -> {
                        log.error("AI 이미지 생성 스트리밍 실패", e);
                        return Flux.just(ServerSentEvent.<String>builder("이미지 생성 중 오류가 발생했습니다.")
                                .event("error")
                                .build());
                    });
        }

        // 2) (텍스트/vision) 답변 조각을 흘려보내면서 전체 답변을 누적한다.
        //    Reactor는 한 구독에 대해 onNext를 직렬로(겹치지 않게) 호출하므로, 누적은 단일 스레드에서만
        //    일어난다. 따라서 동기화 없는 StringBuilder로도 동시성 문제가 없다.(부수효과는 doOnNext에서)
        StringBuilder answer = new StringBuilder();

        // 이어지는 대화면 이전 질문·답변들을 맥락으로 함께 보낸다(후속 질문 이해).
        List<OpenAiClient.Turn> history = loadHistory(request.conversationId());

        // 첫 질문(맥락·첨부 없음)은 기존 텍스트 전용 스트림, 그 외엔 맥락/이미지를 함께 넘긴다.
        Flux<String> rawTokens = (images.isEmpty() && history.isEmpty())
                ? openAiClient.askStream(subject.getCategory(), request.questionText())
                : openAiClient.askStream(subject.getCategory(), history, request.questionText(), toImageInputs(images));

        Flux<ServerSentEvent<String>> tokens = rawTokens
                .doOnNext(answer::append)                              // 전체 답변 누적(부수효과)
                .map(chunk -> ServerSentEvent.builder(chunk).build()); // 토큰 → SSE data 이벤트

        // 3) 스트림이 정상 종료되면 누적된 전체 답변을 저장하고 done 이벤트를 1회 방출한다.
        //    JPA 저장은 블로킹이므로 별도 스케줄러(boundedElastic)에서 수행한다.
        Mono<ServerSentEvent<String>> done = Mono.fromCallable(() -> {
                    AiQuestion saved = persistOnce(savedRef, user, subject, conversation, request.questionText(), answer.toString(), images);
                    return ServerSentEvent.<String>builder(toJson(AiQuestionResponse.from(saved)))
                            .event("done")
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic());

        // 4) 토큰 + done 을 이어붙이고, 도중 오류는 error 이벤트로 변환한다.
        return tokens.concatWith(done)
                // 클라이언트가 도중에 연결을 끊으면 Flux가 취소되어 done이 실행되지 않는다.
                // 이때도 그때까지 받은 답변이 있으면 저장해 질문 유실을 막는다(중복 저장은 persistOnce가 가드).
                .doOnCancel(() -> {
                    if (answer.length() > 0) {
                        Schedulers.boundedElastic().schedule(() ->
                                persistOnce(savedRef, user, subject, conversation, request.questionText(), answer.toString(), images));
                    }
                })
                .onErrorResume(e -> {
                    log.error("AI 스트리밍 실패", e);
                    return Flux.just(ServerSentEvent.<String>builder("AI 풀이 생성 중 오류가 발생했습니다.")
                            .event("error")
                            .build());
                });
    }

    /**
     * 누적 답변을 정확히 한 번만 저장한다.
     *
     * <p>정상 종료(done)와 클라이언트 연결 끊김(doOnCancel)이 경쟁적으로 호출할 수 있으므로,
     * 요청별 {@code savedRef}를 락으로 삼아 첫 호출에서만 실제 저장하고 이후엔 저장된 결과를 재사용한다.</p>
     */
    private AiQuestion persistOnce(AtomicReference<AiQuestion> savedRef, User user, Subject subject,
                                   Conversation conversation, String questionText, String answerText, List<FileAsset> images) {
        synchronized (savedRef) {
            AiQuestion existing = savedRef.get();
            if (existing != null) {
                return existing;
            }
            AiQuestion saved = persist(user, subject, conversation, questionText, answerText, images);
            savedRef.set(saved);
            return saved;
        }
    }

    /**
     * 질문 본문 + 답변 + 첨부 이미지를 하나의 기록으로 저장한다.
     * (attachments는 cascade = ALL 로 함께 insert 된다.)
     */
    private AiQuestion persist(User user, Subject subject, Conversation conversation,
                               String questionText, String answerText, List<FileAsset> images) {
        AiQuestion question = AiQuestion.create(user, subject, conversation, questionText, answerText);
        int order = 0;
        for (FileAsset image : images) {
            // create()가 내부에서 question.addAttachment를 호출 → cascade로 함께 저장됨
            AiQuestionAttachment.create(question, image, order++);
        }
        return aiQuestionRepository.save(question);
    }

    /**
     * 질문이 속할 대화를 결정한다.
     *
     * <ul>
     *   <li>conversationId가 있으면: 본인 소유이고 요청 과목과 같은 대화인지 확인하고 이어쓴다
     *       (없거나, 타인 것이거나, 다른 과목 대화면 404).</li>
     *   <li>conversationId가 없으면: 첫 질문으로 새 대화를 만든다(제목 = 첫 질문 요약).</li>
     * </ul>
     */
    private Conversation resolveConversation(User user, Subject subject, Long conversationId, String firstQuestion) {
        if (conversationId != null) {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ConversationNotFoundException(conversationId));
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new ConversationNotFoundException(conversationId);
            }
            // 대화는 과목에 소속된다 — 다른 과목 대화에 임의로 이어쓰는 것을 막는다.
            // (소유권 검증과 동일하게, 불일치도 존재를 노출하지 않는 404로 처리)
            if (!conversation.getSubject().getId().equals(subject.getId())) {
                throw new ConversationNotFoundException(conversationId);
            }
            return conversation;
        }
        return conversationRepository.save(Conversation.createFromFirstQuestion(user, subject, firstQuestion));
    }

    /** 객체를 JSON 문자열로 직렬화한다(SSE done 이벤트 payload 용). */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            // 직렬화 실패는 사실상 발생하지 않지만, 스트림을 깨뜨리지 않도록 방어한다.
            throw new IllegalStateException("응답 직렬화에 실패했습니다.", e);
        }
    }

    /**
     * 요청의 fileId 목록을 첨부 이미지({@link FileAsset}) 목록으로 변환·검증한다.
     *
     * <p>목록이 비어 있으면 빈 목록(이미지 없는 질문). 각 fileId에 대해 다음을 검증한다:</p>
     * <ul>
     *   <li>존재하는 파일인지</li>
     *   <li>요청한 사용자가 올린 파일인지(타인 파일 도용 방지)</li>
     *   <li>삭제되지 않고 업로드 완료된 사용 가능한 파일인지</li>
     * </ul>
     * 입력 순서를 그대로 유지해 첨부 순서(sortOrder)에 반영한다.
     *
     * @return 검증된 FileAsset 목록 (요청 순서 유지)
     */
    private List<FileAsset> resolveImages(List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyList();
        }
        // fileId별 개별 SELECT(N+1) 대신 단일 IN 쿼리로 한 번에 조회한다. (uploader도 fetch join)
        Map<Long, FileAsset> byId = fileAssetRepository.findByIdInWithUploader(fileIds).stream()
                .collect(Collectors.toMap(FileAsset::getId, image -> image, (a, b) -> a));
        // 요청한 fileIds 순서를 그대로 유지해 첨부 순서(sortOrder)에 반영한다.
        return fileIds.stream()
                .map(fileId -> {
                    FileAsset image = byId.get(fileId);
                    if (image == null) {
                        throw new IllegalArgumentException("첨부 이미지를 찾을 수 없습니다. (fileId: " + fileId + ")");
                    }
                    if (!image.getUploader().getId().equals(userId)) {
                        throw new IllegalArgumentException("본인이 업로드한 이미지만 첨부할 수 있습니다.");
                    }
                    if (!image.isUsable()) {
                        throw new IllegalArgumentException("사용할 수 없는 이미지입니다.");
                    }
                    return image;
                })
                .toList();
    }

    /**
     * 요청 성격에 맞춰 답변(answerText)을 만든다.
     * <ol>
     *   <li>첨부 없음 + 그림 요청 → 이미지 생성 후 마크다운 이미지 답변</li>
     *   <li>첫 질문(맥락·첨부 없음) → 텍스트 전용 답변</li>
     *   <li>그 외 → 이전 맥락(history)·첨부 이미지(vision)를 함께 넘긴 답변</li>
     * </ol>
     */
    private String generateAnswer(Long userId, Subject subject, String questionText,
                                  List<FileAsset> images, List<OpenAiClient.Turn> history) {
        if (images.isEmpty() && isImageGenerationRequest(questionText)) {
            byte[] png = openAiImageClient.generatePng(questionText);
            FileAsset generated = fileService.saveGeneratedImage(userId, png);
            return buildImageAnswerText(generated.getFileUrl());
        }
        if (images.isEmpty() && history.isEmpty()) {
            return openAiClient.ask(subject.getCategory(), questionText);
        }
        return openAiClient.ask(subject.getCategory(), history, questionText, toImageInputs(images));
    }

    /** 한 번의 호출에 맥락으로 보낼 최대 이전 턴 수(질문+답변 쌍 기준). 토큰 비용 폭증 방지. */
    private static final int MAX_HISTORY_TURNS = 10;

    /** 맥락에 다시 실어 보낼 이전 첨부 이미지의 최대 장수(최신 턴 우선). vision 비용 가드. */
    private static final int MAX_HISTORY_IMAGES = 4;

    /**
     * 이어지는 대화의 이전 질문·답변들을 모델에 보낼 맥락으로 변환한다.
     *
     * <p>conversationId가 없으면(새 대화) 빈 목록. 있으면 그 대화의 기록을 오래된 순으로
     * 가져와 최근 {@value #MAX_HISTORY_TURNS}턴만 남긴다. 이전 턴에 첨부됐던 이미지는
     * "아까 그 사진에서 …" 같은 후속 질문을 위해 <b>최신 턴부터</b> 최대
     * {@value #MAX_HISTORY_IMAGES}장까지 다시 실어 보낸다. (소유권은 이 메서드 호출 전에
     * {@link #resolveConversation}에서 이미 검증된다.)</p>
     */
    private List<OpenAiClient.Turn> loadHistory(Long conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        List<AiQuestion> previous = aiQuestionRepository.findWithAttachmentsByConversationId(conversationId);
        if (previous.size() > MAX_HISTORY_TURNS) {
            previous = previous.subList(previous.size() - MAX_HISTORY_TURNS, previous.size());
        }

        // 이전 첨부 이미지는 최신 턴부터 예산 안에서만 다시 보낸다(오래된 이미지일수록 생략).
        Map<Long, List<OpenAiClient.ImageInput>> imagesByQuestionId = new HashMap<>();
        int budget = MAX_HISTORY_IMAGES;
        for (int i = previous.size() - 1; i >= 0 && budget > 0; i--) {
            AiQuestion question = previous.get(i);
            List<OpenAiClient.ImageInput> inputs = new ArrayList<>();
            List<AiQuestionAttachment> attachments = question.getAttachments().stream()
                    .sorted(Comparator.comparingInt(AiQuestionAttachment::getSortOrder))
                    .toList();
            for (AiQuestionAttachment attachment : attachments) {
                if (budget <= 0) {
                    break;
                }
                try {
                    FileAsset asset = attachment.getFileAsset();
                    inputs.add(new OpenAiClient.ImageInput(fileService.readImageBytes(asset), asset.getContentType()));
                    budget--;
                } catch (Exception e) {
                    // 파일 유실 등 — 해당 이미지는 빼고 텍스트 맥락만 유지한다.
                    log.warn("맥락 이미지 로드 실패 (aiQuestionId: {})", question.getId(), e);
                }
            }
            if (!inputs.isEmpty()) {
                imagesByQuestionId.put(question.getId(), inputs);
            }
        }

        return previous.stream()
                .map(q -> new OpenAiClient.Turn(
                        q.getQuestionText(),
                        q.getAnswerText(),
                        imagesByQuestionId.getOrDefault(q.getId(), List.of())))
                .toList();
    }

    /** 첨부 이미지(FileAsset)들을 OpenAI vision 입력(바이트 + MIME)으로 변환한다. */
    private List<OpenAiClient.ImageInput> toImageInputs(List<FileAsset> images) {
        return images.stream()
                .map(image -> new OpenAiClient.ImageInput(
                        fileService.readImageBytes(image),
                        image.getContentType()))
                .toList();
    }

    /** 질문 본문에 "그림/이미지로 답변" 의도가 있는지 키워드로 판별한다. */
    private boolean isImageGenerationRequest(String questionText) {
        if (questionText == null || questionText.isBlank()) {
            return false;
        }
        String text = questionText.toLowerCase();
        return IMAGE_REQUEST_KEYWORDS.stream().anyMatch(text::contains);
    }

    /** 생성된 이미지를 답변으로 보여줄 마크다운 본문을 만든다. (프론트가 이미지로 렌더링) */
    private String buildImageAnswerText(String imageUrl) {
        return "요청하신 이미지를 생성했어요. 🎨\n\n![생성된 이미지](" + imageUrl + ")";
    }

    /**
     * AI 질문 기록 1건 상세 조회. (GET /api/v1/ai/questions/{id})
     *
     * <p>목록 응답에는 답변(answerText)이 빠져 있으므로, 기록을 클릭해 과거 대화(질문+답변)를
     * 복원할 때 이 API로 전체 내용을 가져온다. 본인 소유 기록만 조회할 수 있다.</p>
     *
     * @param userId       인증된 사용자 id
     * @param aiQuestionId 조회할 질문 기록 id
     * @return 질문 + 답변 전체
     */
    @Transactional(readOnly = true)
    public AiQuestionResponse getDetail(Long userId, Long aiQuestionId) {
        AiQuestion question = aiQuestionRepository.findById(aiQuestionId)
                .orElseThrow(() -> new AiQuestionNotFoundException(aiQuestionId));
        // 타인의 기록은 존재 여부를 노출하지 않도록 동일하게 404로 처리한다.
        if (!question.getUser().getId().equals(userId)) {
            throw new AiQuestionNotFoundException(aiQuestionId);
        }
        return AiQuestionResponse.from(question);
    }

    /**
     * 내 AI 질문 기록을 페이징하여 조회한다. (GET /api/v1/ai/questions)
     *
     * @param userId   인증된 사용자 id
     * @param pageable 페이징/정렬 (기본 createdAt DESC, size 20)
     * @return 질문 기록 페이지
     */
    @Transactional(readOnly = true)
    public Page<AiQuestionHistoryResponse> getMyHistory(Long userId, Pageable pageable) {
        return aiQuestionRepository.findHistoryByUserId(userId, pageable)
                .map(AiQuestionHistoryResponse::from);
    }

    /**
     * 내 대화 목록을 조회한다. (GET /api/v1/ai/conversations)
     * subjectId가 있으면 그 과목의 대화만, 없으면 전체를 최신 생성순으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(Long userId, Long subjectId) {
        List<Conversation> conversations = (subjectId == null)
                ? conversationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : conversationRepository.findByUserIdAndSubjectIdOrderByCreatedAtDesc(userId, subjectId);
        return conversations.stream().map(ConversationSummaryResponse::from).toList();
    }

    /**
     * 대화를 삭제한다. (DELETE /api/v1/ai/conversations/{id})
     *
     * <p>본인 소유 대화만 삭제할 수 있으며(타인 것은 존재 노출 없이 동일하게 404),
     * 그 대화의 질문·답변 기록과 첨부 연결({@code ai_question_attachment})까지 함께 지운다.
     * 첨부의 원본 파일({@code file_asset}/실제 파일)은 다른 곳에서 참조될 수 있어 남겨둔다.</p>
     */
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        if (!conversation.getUser().getId().equals(userId)) {
            throw new ConversationNotFoundException(conversationId);
        }
        // 엔티티 단위 deleteAll(SELECT N + DELETE 2N) 대신 벌크 DELETE 2방으로 정리한다.
        // FK 제약 순서: 첨부 연결 → 질문 → 대화.
        aiQuestionRepository.deleteAttachmentsByConversationId(conversationId);
        aiQuestionRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    /**
     * 대화 상세(질문+답변 전체)를 조회한다. (GET /api/v1/ai/conversations/{id})
     * 본인 소유 대화만 조회할 수 있다.
     */
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetail(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        if (!conversation.getUser().getId().equals(userId)) {
            throw new ConversationNotFoundException(conversationId);
        }
        List<AiQuestionResponse> questions = aiQuestionRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(AiQuestionResponse::from)
                .toList();
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getSubject().getId(),
                questions
        );
    }
}
