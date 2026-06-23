package com.studyflow.domain.qna.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.naegong.enums.NaegongReason;
import com.studyflow.domain.naegong.service.NaegongService;
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.event.NotificationCreatedEvent;
import com.studyflow.domain.qna.dto.request.QnaAnswerRequest;
import com.studyflow.domain.qna.dto.request.QnaBlockRequest;
import com.studyflow.domain.qna.dto.request.QnaQuestionCreateRequest;
import com.studyflow.domain.qna.dto.request.QnaQuestionUpdateRequest;
import com.studyflow.domain.qna.dto.response.*;
import com.studyflow.domain.qna.entity.QnaAnswer;
import com.studyflow.domain.qna.entity.QnaAnswerAttachment;
import com.studyflow.domain.qna.entity.QnaAnswerLike;
import com.studyflow.domain.qna.entity.QnaQuestion;
import com.studyflow.domain.qna.entity.QnaQuestionAttachment;
import com.studyflow.domain.qna.exception.QnaAnswerNotFoundException;
import com.studyflow.domain.qna.exception.QnaForbiddenException;
import com.studyflow.domain.qna.exception.QnaInvalidStateException;
import com.studyflow.domain.qna.exception.QnaQuestionNotFoundException;
import com.studyflow.domain.qna.repository.QnaAnswerAttachmentRepository;
import com.studyflow.domain.qna.repository.QnaAnswerLikeRepository;
import com.studyflow.domain.qna.repository.QnaAnswerRepository;
import com.studyflow.domain.qna.repository.QnaAnswerRepositoryCustom;
import com.studyflow.domain.qna.repository.QnaQuestionAttachmentRepository;
import com.studyflow.domain.qna.repository.QnaQuestionRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.exception.SubjectNotFoundException;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * QnA(질문게시판) 도메인 서비스.
 *
 * <p>지식인 스타일 흐름을 담당한다.</p>
 * <ul>
 *   <li>질문: STUDENT만 작성, 작성자 본인만 수정/삭제</li>
 *   <li>답변: TEACHER만 작성, 작성자 본인만 수정/삭제</li>
 *   <li>좋아요: 로그인 사용자면 누구나</li>
 *   <li>채택: 질문 작성 학생만 가능 → 채택 시 질문 해결 처리 + 답변 선생님 내공 적립</li>
 * </ul>
 *
 * <p>질문·답변 모두 이미지를 여러 장 첨부할 수 있다. 첨부는 먼저 파일 업로드 API로 올린
 * fileId 목록을 받아 {@link FileAsset}을 검증한 뒤 attachment로 연결한다(AI 질문과 동일).</p>
 *
 * <p>역할(STUDENT/TEACHER) 1차 검증은 SecurityConfig의 경로별 규칙이 담당하고,
 * 소유권(본인 글 여부)·상태(이미 채택됨 등) 검증은 이 서비스가 담당한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QnaService {

    // 답변 채택 시 선생님에게 적립되는 내공 점수
    private static final int ACCEPT_ANSWER_NAEGONG_SCORE = 10;

    // 목록 정렬 파라미터 중 '답변 많은순'을 가리키는 키. (오타/이름 변경 시 컴파일에서 잡히도록 상수화)
    private static final String SORT_ANSWER_COUNT = "answerCount";

    // 본문 블록(content_json) 역직렬화용 타입 토큰
    private static final TypeReference<List<QnaBlockRequest>> BLOCK_LIST_TYPE = new TypeReference<>() {
    };

    private final QnaQuestionRepository questionRepository;
    private final QnaAnswerRepository answerRepository;
    private final QnaQuestionAttachmentRepository questionAttachmentRepository;
    private final QnaAnswerAttachmentRepository answerAttachmentRepository;
    private final QnaAnswerLikeRepository likeRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final FileAssetRepository fileAssetRepository;
    private final NaegongService naegongService;
    // 본문 블록 직렬화/역직렬화용. Spring이 관리하는 빈을 주입받아 앱 Jackson 설정을 동일하게 따른다.
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ── 질문 ────────────────────────────────────────────────

    /** 질문 목록 조회 (Public). 필터: 과목/검색어/해결여부. 답변 개수는 페이지 단위로 일괄 집계한다. */
    @Transactional(readOnly = true)
    public Page<QnaQuestionSummaryResponse> getQuestions(Long subjectId, String keyword, Boolean resolved,
                                                         Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        // 정렬이 'answerCount'(답변 많은순)이면 집계 기반 전용 쿼리로, 그 외(createdAt/viewCount)는 일반 정렬로 조회
        boolean orderByAnswers = pageable.getSort().stream()
                .anyMatch(o -> SORT_ANSWER_COUNT.equals(o.getProperty()));
        Page<QnaQuestion> page = orderByAnswers
                ? questionRepository.findFilteredOrderByAnswerCount(subjectId, normalizedKeyword, resolved, pageable)
                : questionRepository.findFiltered(subjectId, normalizedKeyword, resolved, pageable);

        List<Long> questionIds = page.getContent().stream().map(QnaQuestion::getId).toList();
        Map<Long, Long> answerCounts = questionIds.isEmpty()
                ? Collections.emptyMap()
                : answerRepository.countByQuestionIds(questionIds).stream()
                        .collect(Collectors.toMap(
                                QnaAnswerRepositoryCustom.QuestionAnswerCount::questionId,
                                QnaAnswerRepositoryCustom.QuestionAnswerCount::cnt));

        // 카드 썸네일용: 질문별 첫 번째 첨부 이미지 URL을 일괄 조회
        Map<Long, String> thumbnails = questionIds.isEmpty()
                ? Collections.emptyMap()
                : questionAttachmentRepository.findThumbnailsByQuestionIds(questionIds).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuestion().getId(),
                                a -> a.getFileAsset().getFileUrl(),
                                (a, b) -> a));

        return page.map(q -> QnaQuestionSummaryResponse.of(
                q, answerCounts.getOrDefault(q.getId(), 0L), thumbnails.get(q.getId())));
    }

    /** 질문게시판 전역 통계 (목록 상단 카드용). 필터와 무관하다. */
    @Transactional(readOnly = true)
    public QnaBoardStatsResponse getBoardStats() {
        long total = questionRepository.count();
        long resolved = questionRepository.countByResolved(true);
        long totalAnswers = answerRepository.count();
        if (resolved > total) {
            // count()와 countByResolved() 사이 동시 변경으로 인한 일시적 불일치 — 감지 가능하도록 로그
            log.warn("QnA 통계 불일치 감지: resolved({}) > total({})", resolved, total);
        }
        return QnaBoardStatsResponse.of(total, resolved, totalAnswers);
    }

    /** 질문 상세 조회 (Public). 조회 시 조회수를 1 증가시킨다. */
    @Transactional
    public QnaQuestionDetailResponse getQuestionDetail(Long questionId, Long currentUserId) {
        QnaQuestion question = questionRepository.findDetailById(questionId)
                .orElseThrow(() -> new QnaQuestionNotFoundException(questionId));
        question.increaseViewCount();

        List<QnaQuestionAttachment> questionAttachments = questionAttachmentRepository.findByQuestionIdWithFile(questionId);
        List<QnaImageResponse> questionImages = questionAttachments.stream()
                .map(a -> new QnaImageResponse(a.getFileAsset().getId(), a.getFileAsset().getFileUrl()))
                .toList();
        Map<Long, String> questionUrlByFileId = questionAttachments.stream()
                .collect(Collectors.toMap(a -> a.getFileAsset().getId(), a -> a.getFileAsset().getFileUrl(), (a, b) -> a));
        List<QnaBlockResponse> questionBlocks = buildContentBlocks(question.getContentJson(), questionUrlByFileId);

        List<QnaAnswer> answers = answerRepository.findByQuestionIdWithAuthor(questionId);
        List<QnaAnswerResponse> answerResponses = buildAnswerResponses(answers, currentUserId);

        return QnaQuestionDetailResponse.of(question, questionImages, questionBlocks, answerResponses);
    }

    /** 질문 작성 (STUDENT). */
    @Transactional
    public QnaQuestionCreateResponse createQuestion(Long userId, QnaQuestionCreateRequest request) {
        Subject subject = getSubject(request.getSubjectId());
        User author = userRepository.getReferenceById(userId);

        QnaQuestion question = QnaQuestion.create(author, subject, request.getTitle(), request.getContent());

        // 블록 에디터로 작성된 경우: 블록 JSON을 저장하고 이미지 첨부는 블록의 image 블록에서 도출한다.
        List<Long> imageFileIds = request.getImageFileIds();
        if (request.getBlocks() != null) {
            ContentParts parts = parseBlocks(request.getBlocks());
            question.applyContentJson(parts.json());
            imageFileIds = parts.imageFileIds();
        }
        attachQuestionImages(question, imageFileIds, userId);
        questionRepository.save(question);
        return QnaQuestionCreateResponse.from(question);
    }

    /**
     * 질문 수정 (작성 학생 본인).
     * <p>이미지(imageFileIds) 정책: null이면 <b>기존 첨부 유지</b>(텍스트만 수정), 배열이 오면 그 목록으로
     * 전체 교체(빈 배열이면 모두 제거). 일부만 남기고 일부만 삭제·추가하려면 FE가 상세 응답의
     * fileId들 중 남길 것 + 새로 올린 fileId를 합쳐 보내면 된다(이미 본인이 올린 fileId는 재첨부 가능).</p>
     */
    @Transactional
    public void updateQuestion(Long userId, Long questionId, QnaQuestionUpdateRequest request) {
        QnaQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QnaQuestionNotFoundException(questionId));
        requireAuthor(question.isAuthor(userId), "본인이 작성한 질문만 수정할 수 있습니다.");

        Subject subject = getSubject(request.getSubjectId());
        question.update(subject, request.getTitle(), request.getContent());

        if (request.getBlocks() != null) {
            // 블록 에디터 수정: 블록 JSON을 갱신하고 이미지는 블록 기준으로 전체 교체한다.
            ContentParts parts = parseBlocks(request.getBlocks());
            question.applyContentJson(parts.json());
            replaceQuestionImages(question, parts.imageFileIds(), userId);
        } else if (request.getImageFileIds() != null) {
            // 레거시(블록 없음): imageFileIds == null이면 기존 이미지 유지, 배열이면 전체 교체
            replaceQuestionImages(question, request.getImageFileIds(), userId);
        }
    }

    /** 질문 삭제 (작성 학생 본인 또는 관리자). 답변·좋아요·첨부는 cascade로 함께 삭제된다. */
    @Transactional
    public void deleteQuestion(Long userId, Long questionId, boolean isAdmin) {
        QnaQuestion question = questionRepository.findByIdWithLock(questionId)
                .orElseThrow(() -> new QnaQuestionNotFoundException(questionId));
        requireAuthor(question.isAuthor(userId) || isAdmin, "본인이 작성한 질문만 삭제할 수 있습니다.");
        questionRepository.delete(question);
    }

    // ── 답변 ────────────────────────────────────────────────

    /** 답변 작성 (TEACHER). */
    @Transactional
    public QnaAnswerCreateResponse createAnswer(Long userId, Long questionId, QnaAnswerRequest request) {
        QnaQuestion question = questionRepository.findByIdWithLock(questionId)
                .orElseThrow(() -> new QnaQuestionNotFoundException(questionId));
        User author = userRepository.getReferenceById(userId);

        QnaAnswer answer = QnaAnswer.create(question, author, request.getContent());

        List<Long> imageFileIds = request.getImageFileIds();
        if (request.getBlocks() != null) {
            ContentParts parts = parseBlocks(request.getBlocks());
            answer.applyContentJson(parts.json());
            imageFileIds = parts.imageFileIds();
        }
        attachAnswerImages(answer, imageFileIds, userId);
        answerRepository.save(answer);

        // 질문 작성 학생에게 답변 알림 (본인 질문에 본인이 답변하는 경우는 제외 — 일반적으로 발생하지 않음)
        Long questionAuthorId = question.getAuthor().getId();
        if (!questionAuthorId.equals(userId)) {
            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    questionAuthorId, NotificationType.QNA_ANSWERED,
                    "새 답변",
                    String.format("내 질문 '%s'에 답변이 달렸어요.", question.getTitle()),
                    question.getId()));
        }

        return QnaAnswerCreateResponse.from(answer);
    }

    /**
     * 답변 수정 (작성 선생님 본인).
     * <p>이미지(imageFileIds) 정책: null이면 기존 첨부 유지(텍스트만 수정), 배열이 오면 전체 교체.</p>
     */
    @Transactional
    public void updateAnswer(Long userId, Long answerId, QnaAnswerRequest request) {
        QnaAnswer answer = answerRepository.findDetailById(answerId)
                .orElseThrow(() -> new QnaAnswerNotFoundException(answerId));
        requireAuthor(answer.isAuthor(userId), "본인이 작성한 답변만 수정할 수 있습니다.");
        answer.updateContent(request.getContent());

        if (request.getBlocks() != null) {
            ContentParts parts = parseBlocks(request.getBlocks());
            answer.applyContentJson(parts.json());
            replaceAnswerImages(answer, parts.imageFileIds(), userId);
        } else if (request.getImageFileIds() != null) {
            replaceAnswerImages(answer, request.getImageFileIds(), userId);
        }
    }

    /**
     * 답변 삭제 (작성 선생님 본인 또는 관리자).
     * 채택된 답변이 삭제되면 질문의 해결 상태를 되돌린다(이미 적립된 내공은 회수하지 않음).
     */
    @Transactional
    public void deleteAnswer(Long userId, Long answerId, boolean isAdmin) {
        QnaAnswer answer = answerRepository.findDetailById(answerId)
                .orElseThrow(() -> new QnaAnswerNotFoundException(answerId));
        requireAuthor(answer.isAuthor(userId) || isAdmin, "본인이 작성한 답변만 삭제할 수 있습니다.");

        if (answer.isAccepted()) {
            answer.getQuestion().unmarkResolved();
        }
        answerRepository.delete(answer);
    }

    /** 답변 채택 (질문 작성 학생 본인). 채택 시 질문 해결 + 답변 선생님 내공 적립. */
    @Transactional
    public QnaAnswerAcceptResponse acceptAnswer(Long userId, Long answerId) {
        QnaAnswer answer = answerRepository.findDetailById(answerId)
                .orElseThrow(() -> new QnaAnswerNotFoundException(answerId));
        QnaQuestion question = answer.getQuestion();

        if (!question.isAuthor(userId)) {
            throw new QnaForbiddenException("질문 작성자만 답변을 채택할 수 있습니다.");
        }
        if (question.isResolved()) {
            throw new QnaInvalidStateException(ErrorCode.QNA_ALREADY_RESOLVED,
                    "이미 답변이 채택된 질문입니다.");
        }

        answer.accept();
        question.markResolved();

        User teacher = answer.getAuthor();
        int teacherNaegongScore = naegongService.addScore(
                teacher, ACCEPT_ANSWER_NAEGONG_SCORE, NaegongReason.ANSWER_ACCEPTED, answer.getId());

        // 채택된 답변 작성 선생님에게 알림(클릭 시 /qna/{questionId} 이동). 본인 채택 자기알림 방지.
        if (!teacher.getId().equals(userId)) {
            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    teacher.getId(), NotificationType.QNA_ANSWER_ACCEPTED,
                    "답변이 채택되었어요",
                    "'" + question.getTitle() + "' 질문에서 회원님의 답변이 채택되었어요.",
                    question.getId()));
        }

        return new QnaAnswerAcceptResponse(
                answer.getId(),
                question.getId(),
                teacher.getId(),
                true,
                true,
                ACCEPT_ANSWER_NAEGONG_SCORE,
                teacherNaegongScore);
    }

    // ── 좋아요 ──────────────────────────────────────────────

    /** 답변 좋아요 토글 (로그인 사용자). 이미 눌렀으면 취소, 아니면 추가. */
    @Transactional
    public QnaLikeResponse toggleAnswerLike(Long userId, Long answerId) {
        QnaAnswer answer = answerRepository.findByIdWithLock(answerId)
                .orElseThrow(() -> new QnaAnswerNotFoundException(answerId));

        boolean liked;
        var existing = likeRepository.findByAnswerIdAndUserId(answerId, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            answer.decreaseLikeCount();
            liked = false;
        } else {
            likeRepository.save(QnaAnswerLike.create(answer, userRepository.getReferenceById(userId)));
            answer.increaseLikeCount();
            liked = true;
        }
        return new QnaLikeResponse(answerId, liked, answer.getLikeCount());
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────

    /** 답변 목록 → 응답 변환. 첨부 이미지/좋아요 여부를 답변 id 기준으로 일괄 조회하여 N+1을 피한다. */
    private List<QnaAnswerResponse> buildAnswerResponses(List<QnaAnswer> answers, Long currentUserId) {
        if (answers.isEmpty()) {
            return List.of();
        }
        List<Long> answerIds = answers.stream().map(QnaAnswer::getId).toList();

        // 답변별 첨부 (sortOrder 순). 쿼리가 sortOrder ASC로 정렬되어 그룹 내 순서가 보존된다.
        Map<Long, List<QnaAnswerAttachment>> attachmentsByAnswer = answerAttachmentRepository.findByAnswerIdsWithFile(answerIds)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getAnswer().getId()));

        Set<Long> likedAnswerIds = (currentUserId == null)
                ? Collections.emptySet()
                : Set.copyOf(likeRepository.findLikedAnswerIds(currentUserId, answerIds));

        return answers.stream()
                .map(a -> {
                    List<QnaAnswerAttachment> atts = attachmentsByAnswer.getOrDefault(a.getId(), List.of());
                    List<QnaImageResponse> images = atts.stream()
                            .map(x -> new QnaImageResponse(x.getFileAsset().getId(), x.getFileAsset().getFileUrl()))
                            .toList();
                    Map<Long, String> urlByFileId = atts.stream()
                            .collect(Collectors.toMap(x -> x.getFileAsset().getId(), x -> x.getFileAsset().getFileUrl(), (p, q) -> p));
                    List<QnaBlockResponse> blocks = buildContentBlocks(a.getContentJson(), urlByFileId);
                    return QnaAnswerResponse.of(a, likedAnswerIds.contains(a.getId()), images, blocks);
                })
                .toList();
    }

    /** 질문 이미지를 전체 교체한다. (기존 첨부 제거 후 새 목록 연결 — unique(question_id,file_id) 충돌 방지) */
    private void replaceQuestionImages(QnaQuestion question, List<Long> fileIds, Long userId) {
        if (!question.getAttachments().isEmpty()) {
            question.clearAttachments();
            questionAttachmentRepository.flush();
        }
        attachQuestionImages(question, fileIds, userId);
    }

    /** 답변 이미지를 전체 교체한다. */
    private void replaceAnswerImages(QnaAnswer answer, List<Long> fileIds, Long userId) {
        if (!answer.getAttachments().isEmpty()) {
            answer.clearAttachments();
            answerAttachmentRepository.flush();
        }
        attachAnswerImages(answer, fileIds, userId);
    }

    /**
     * 요청 본문 블록을 검증·정규화해 저장용 JSON과 이미지 fileId 목록으로 변환한다.
     *
     * <p>text 블록은 text만, image 블록은 fileId만 남겨 직렬화한다(url은 저장하지 않음 — 조회 시 fileId로 해석).
     * image 블록에 fileId가 없으면 400.</p>
     */
    private ContentParts parseBlocks(List<QnaBlockRequest> blocks) {
        List<QnaBlockRequest> normalized = new ArrayList<>();
        List<Long> imageFileIds = new ArrayList<>();
        for (QnaBlockRequest b : blocks) {
            if (b == null || b.type() == null) {
                continue;
            }
            switch (b.type()) {
                case "text" -> normalized.add(new QnaBlockRequest("text", b.text() == null ? "" : b.text(), null));
                case "image" -> {
                    if (b.fileId() == null) {
                        throw new IllegalArgumentException("이미지 블록에 fileId가 없습니다.");
                    }
                    normalized.add(new QnaBlockRequest("image", null, b.fileId()));
                    imageFileIds.add(b.fileId());
                }
                default -> throw new IllegalArgumentException("알 수 없는 블록 타입: " + b.type());
            }
        }
        try {
            return new ContentParts(objectMapper.writeValueAsString(normalized), imageFileIds);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("본문 블록 직렬화에 실패했습니다.", e);
        }
    }

    /**
     * 저장된 content_json을 응답 블록 목록으로 변환한다. image 블록의 url은 fileId→url 맵으로 해석한다.
     * json이 없거나(레거시) 파싱 불가하면 null을 반환해 FE가 content/imageUrls로 폴백하게 한다.
     */
    private List<QnaBlockResponse> buildContentBlocks(String contentJson, Map<Long, String> urlByFileId) {
        if (contentJson == null || contentJson.isBlank()) {
            return null;
        }
        List<QnaBlockRequest> stored;
        try {
            stored = objectMapper.readValue(contentJson, BLOCK_LIST_TYPE);
        } catch (JsonProcessingException e) {
            // 저장된 본문 블록 JSON이 손상된 경우: 폴백(null) 반환하되 감지 가능하도록 로그를 남긴다.
            log.warn("content_json 파싱 실패 — 블록 없이 폴백 렌더합니다. json={}", contentJson, e);
            return null;
        }
        return stored.stream()
                .map(b -> "image".equals(b.type())
                        ? new QnaBlockResponse("image", null, b.fileId(), urlByFileId.get(b.fileId()))
                        : new QnaBlockResponse("text", b.text(), null, null))
                .toList();
    }

    /** 본문 블록 파싱 결과: 저장용 JSON + 이미지 fileId 목록(순서 보존). */
    private record ContentParts(String json, List<Long> imageFileIds) {
    }

    /** 질문에 첨부 이미지를 연결한다. (요청 fileId 순서를 sortOrder로 보존) */
    private void attachQuestionImages(QnaQuestion question, List<Long> fileIds, Long userId) {
        int order = 0;
        for (FileAsset image : resolveImages(fileIds, userId)) {
            QnaQuestionAttachment.create(question, image, order++);
        }
    }

    /** 답변에 첨부 이미지를 연결한다. */
    private void attachAnswerImages(QnaAnswer answer, List<Long> fileIds, Long userId) {
        int order = 0;
        for (FileAsset image : resolveImages(fileIds, userId)) {
            QnaAnswerAttachment.create(answer, image, order++);
        }
    }

    /**
     * 요청 fileId 목록을 첨부 이미지({@link FileAsset}) 목록으로 변환·검증한다(요청 순서 유지).
     *
     * <p>각 fileId에 대해 존재 여부 / 본인 업로드 여부 / 사용 가능(미삭제·업로드 완료) 여부를 검증한다.
     * 위반 시 {@link IllegalArgumentException}(→ 400). AI 질문의 동일 로직과 정책을 맞춘다.</p>
     */
    private List<FileAsset> resolveImages(List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        Map<Long, FileAsset> byId = fileAssetRepository.findByIdInWithUploader(fileIds).stream()
                .collect(Collectors.toMap(FileAsset::getId, image -> image, (a, b) -> a));
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

    private Subject getSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));
    }

    private void requireAuthor(boolean allowed, String message) {
        if (!allowed) {
            throw new QnaForbiddenException(message);
        }
    }
}
