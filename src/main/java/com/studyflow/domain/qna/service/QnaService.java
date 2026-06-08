package com.studyflow.domain.qna.service;

import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.naegong.enums.NaegongReason;
import com.studyflow.domain.naegong.service.NaegongService;
import com.studyflow.domain.qna.dto.request.QnaAnswerRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
public class QnaService {

    // 답변 채택 시 선생님에게 적립되는 내공 점수
    private static final int ACCEPT_ANSWER_NAEGONG_SCORE = 10;

    private final QnaQuestionRepository questionRepository;
    private final QnaAnswerRepository answerRepository;
    private final QnaQuestionAttachmentRepository questionAttachmentRepository;
    private final QnaAnswerAttachmentRepository answerAttachmentRepository;
    private final QnaAnswerLikeRepository likeRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final FileAssetRepository fileAssetRepository;
    private final NaegongService naegongService;

    // ── 질문 ────────────────────────────────────────────────

    /** 질문 목록 조회 (Public). 필터: 과목/검색어/해결여부. 답변 개수는 페이지 단위로 일괄 집계한다. */
    @Transactional(readOnly = true)
    public Page<QnaQuestionSummaryResponse> getQuestions(Long subjectId, String keyword, Boolean resolved,
                                                         Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<QnaQuestion> page = questionRepository.findFiltered(subjectId, normalizedKeyword, resolved, pageable);

        List<Long> questionIds = page.getContent().stream().map(QnaQuestion::getId).toList();
        Map<Long, Long> answerCounts = questionIds.isEmpty()
                ? Collections.emptyMap()
                : answerRepository.countByQuestionIds(questionIds).stream()
                        .collect(Collectors.toMap(
                                QnaAnswerRepositoryCustom.QuestionAnswerCount::questionId,
                                QnaAnswerRepositoryCustom.QuestionAnswerCount::cnt));

        return page.map(q -> QnaQuestionSummaryResponse.of(q, answerCounts.getOrDefault(q.getId(), 0L)));
    }

    /** 질문 상세 조회 (Public). 조회 시 조회수를 1 증가시킨다. */
    @Transactional
    public QnaQuestionDetailResponse getQuestionDetail(Long questionId, Long currentUserId) {
        QnaQuestion question = questionRepository.findDetailById(questionId)
                .orElseThrow(() -> new QnaQuestionNotFoundException(questionId));
        question.increaseViewCount();

        List<String> questionImageUrls = questionAttachmentRepository.findByQuestionIdWithFile(questionId).stream()
                .map(a -> a.getFileAsset().getFileUrl())
                .toList();

        List<QnaAnswer> answers = answerRepository.findByQuestionIdWithAuthor(questionId);
        List<QnaAnswerResponse> answerResponses = buildAnswerResponses(answers, currentUserId);

        return QnaQuestionDetailResponse.of(question, questionImageUrls, answerResponses);
    }

    /** 질문 작성 (STUDENT). */
    @Transactional
    public QnaQuestionCreateResponse createQuestion(Long userId, QnaQuestionCreateRequest request) {
        Subject subject = getSubject(request.getSubjectId());
        User author = userRepository.getReferenceById(userId);

        QnaQuestion question = QnaQuestion.create(author, subject, request.getTitle(), request.getContent());
        attachQuestionImages(question, request.getImageFileIds(), userId);
        questionRepository.save(question);
        return QnaQuestionCreateResponse.from(question);
    }

    /** 질문 수정 (작성 학생 본인). 이미지는 요청 목록으로 전체 교체한다. */
    @Transactional
    public void updateQuestion(Long userId, Long questionId, QnaQuestionUpdateRequest request) {
        QnaQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QnaQuestionNotFoundException(questionId));
        requireAuthor(question.isAuthor(userId), "본인이 작성한 질문만 수정할 수 있습니다.");

        Subject subject = getSubject(request.getSubjectId());
        question.update(subject, request.getTitle(), request.getContent());

        // 기존 첨부 제거(orphan 삭제를 먼저 flush) 후 새 목록으로 교체 — unique(question_id,file_id) 충돌 방지
        if (!question.getAttachments().isEmpty()) {
            question.clearAttachments();
            questionAttachmentRepository.flush();
        }
        attachQuestionImages(question, request.getImageFileIds(), userId);
    }

    /** 질문 삭제 (작성 학생 본인 또는 관리자). 답변·좋아요·첨부는 cascade로 함께 삭제된다. */
    @Transactional
    public void deleteQuestion(Long userId, Long questionId, boolean isAdmin) {
        QnaQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QnaQuestionNotFoundException(questionId));
        requireAuthor(question.isAuthor(userId) || isAdmin, "본인이 작성한 질문만 삭제할 수 있습니다.");
        questionRepository.delete(question);
    }

    // ── 답변 ────────────────────────────────────────────────

    /** 답변 작성 (TEACHER). */
    @Transactional
    public QnaAnswerCreateResponse createAnswer(Long userId, Long questionId, QnaAnswerRequest request) {
        QnaQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QnaQuestionNotFoundException(questionId));
        User author = userRepository.getReferenceById(userId);

        QnaAnswer answer = QnaAnswer.create(question, author, request.getContent());
        attachAnswerImages(answer, request.getImageFileIds(), userId);
        answerRepository.save(answer);
        return QnaAnswerCreateResponse.from(answer);
    }

    /** 답변 수정 (작성 선생님 본인). 이미지는 요청 목록으로 전체 교체한다. */
    @Transactional
    public void updateAnswer(Long userId, Long answerId, QnaAnswerRequest request) {
        QnaAnswer answer = answerRepository.findDetailById(answerId)
                .orElseThrow(() -> new QnaAnswerNotFoundException(answerId));
        requireAuthor(answer.isAuthor(userId), "본인이 작성한 답변만 수정할 수 있습니다.");
        answer.updateContent(request.getContent());

        if (!answer.getAttachments().isEmpty()) {
            answer.clearAttachments();
            answerAttachmentRepository.flush();
        }
        attachAnswerImages(answer, request.getImageFileIds(), userId);
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
        QnaAnswer answer = answerRepository.findById(answerId)
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

        // 답변별 첨부 이미지 URL (sortOrder 순). 쿼리가 sortOrder ASC로 정렬되어 그룹 내 순서가 보존된다.
        Map<Long, List<String>> imageUrlsByAnswer = answerAttachmentRepository.findByAnswerIdsWithFile(answerIds)
                .stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAnswer().getId(),
                        Collectors.mapping(a -> a.getFileAsset().getFileUrl(), Collectors.toList())));

        Set<Long> likedAnswerIds = (currentUserId == null)
                ? Collections.emptySet()
                : Set.copyOf(likeRepository.findLikedAnswerIds(currentUserId, answerIds));

        return answers.stream()
                .map(a -> QnaAnswerResponse.of(
                        a,
                        likedAnswerIds.contains(a.getId()),
                        imageUrlsByAnswer.getOrDefault(a.getId(), List.of())))
                .toList();
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
