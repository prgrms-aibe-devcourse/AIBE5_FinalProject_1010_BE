package com.studyflow.domain.ai.service;

import com.studyflow.domain.ai.client.OpenAiClient;
import com.studyflow.domain.ai.dto.request.AiQuestionCreateRequest;
import com.studyflow.domain.ai.dto.response.AiQuestionHistoryResponse;
import com.studyflow.domain.ai.dto.response.AiQuestionResponse;
import com.studyflow.domain.ai.entity.AiQuestion;
import com.studyflow.domain.ai.entity.AiQuestionAttachment;
import com.studyflow.domain.ai.exception.SubjectNotFoundException;
import com.studyflow.domain.ai.repository.AiQuestionRepository;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.subject.entity.Subject;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final FileAssetRepository fileAssetRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

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

        // 2) 첨부 이미지(선택, 여러 장) 해석: fileId 목록을 FileAsset 목록으로 변환·검증한다.
        List<FileAsset> images = resolveImages(request.questionImageFileIds(), userId);

        // 3) OpenAI 호출로 답변 생성 — 트랜잭션 밖에서 수행(커넥션 점유 방지)
        //    과목 대분류를 함께 넘겨, 선택한 과목 특성에 맞는 풀이가 나오도록 한다.
        //    (1단계: 텍스트만 전달. 2단계에서 images의 URL들을 vision으로 함께 넘기도록 확장)
        String answerText = openAiClient.ask(subject.getCategory(), request.questionText());

        // 4) 질문 + 답변 + 첨부를 저장하고 응답으로 변환한다.
        AiQuestion saved = persist(user, subject, request.questionText(), answerText, images);
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
    public Flux<ServerSentEvent<String>> askStream(Long userId, AiQuestionCreateRequest request) {
        // 1) 스트림 시작 전에 동기로 검증한다(여기서 던진 예외는 GlobalExceptionHandler가 처리).
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));
        List<FileAsset> images = resolveImages(request.questionImageFileIds(), userId);

        // 2) 답변 조각을 흘려보내면서 전체 답변을 누적한다.
        StringBuilder answer = new StringBuilder();
        Flux<ServerSentEvent<String>> tokens = openAiClient
                .askStream(subject.getCategory(), request.questionText())
                .map(chunk -> {
                    answer.append(chunk);
                    return ServerSentEvent.builder(chunk).build();
                });

        // 3) 스트림이 끝나면 누적된 전체 답변을 저장하고 done 이벤트를 1회 방출한다.
        //    JPA 저장은 블로킹이므로 별도 스케줄러(boundedElastic)에서 수행한다.
        Mono<ServerSentEvent<String>> done = Mono.fromCallable(() -> {
                    AiQuestion saved = persist(user, subject, request.questionText(), answer.toString(), images);
                    return ServerSentEvent.<String>builder(toJson(AiQuestionResponse.from(saved)))
                            .event("done")
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic());

        // 4) 토큰 + done 을 이어붙이고, 도중 오류는 error 이벤트로 변환한다.
        return tokens.concatWith(done)
                .onErrorResume(e -> {
                    log.error("AI 스트리밍 실패", e);
                    return Flux.just(ServerSentEvent.<String>builder("AI 풀이 생성 중 오류가 발생했습니다.")
                            .event("error")
                            .build());
                });
    }

    /**
     * 질문 본문 + 답변 + 첨부 이미지를 하나의 기록으로 저장한다.
     * (attachments는 cascade = ALL 로 함께 insert 된다.)
     */
    private AiQuestion persist(User user, Subject subject, String questionText, String answerText, List<FileAsset> images) {
        AiQuestion question = AiQuestion.create(user, subject, questionText, answerText);
        int order = 0;
        for (FileAsset image : images) {
            // create()가 내부에서 question.addAttachment를 호출 → cascade로 함께 저장됨
            AiQuestionAttachment.create(question, image, order++);
        }
        return aiQuestionRepository.save(question);
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
}
