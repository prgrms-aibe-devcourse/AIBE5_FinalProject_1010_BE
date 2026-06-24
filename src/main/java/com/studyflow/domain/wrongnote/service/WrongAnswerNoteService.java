package com.studyflow.domain.wrongnote.service;

import com.studyflow.domain.ai.entity.AiQuestion;
import com.studyflow.domain.ai.repository.AiQuestionRepository;
import com.studyflow.domain.qna.entity.QnaAnswer;
import com.studyflow.domain.qna.entity.QnaQuestion;
import com.studyflow.domain.qna.repository.QnaAnswerRepository;
import com.studyflow.domain.qna.repository.QnaQuestionRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.exception.SubjectNotFoundException;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteReviewRequest;
import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteCreateRequest;
import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteUpdateRequest;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNoteAnswerResponse;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNotePracticeResponse;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNoteReviewResponse;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNoteResponse;
import com.studyflow.domain.wrongnote.entity.WrongAnswerNote;
import com.studyflow.domain.wrongnote.entity.WrongAnswerNoteReview;
import com.studyflow.domain.wrongnote.enums.WrongAnswerReviewResult;
import com.studyflow.domain.wrongnote.enums.WrongAnswerSourceType;
import com.studyflow.domain.wrongnote.repository.WrongAnswerNoteReviewRepository;
import com.studyflow.domain.wrongnote.repository.WrongAnswerNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class WrongAnswerNoteService {

    private static final int MAX_TAG_COUNT = 20;
    private static final int MAX_PRACTICE_SIZE = 50;

    private final WrongAnswerNoteRepository noteRepository;
    private final WrongAnswerNoteReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final QnaQuestionRepository qnaQuestionRepository;
    private final QnaAnswerRepository qnaAnswerRepository;
    private final AiQuestionRepository aiQuestionRepository;

    @Transactional(readOnly = true)
    public Page<WrongAnswerNoteResponse> getMyNotes(Long userId, Long subjectId, String keyword, Pageable pageable) {
        return noteRepository.searchMine(userId, subjectId, normalizeKeyword(keyword), pageable)
                .map(WrongAnswerNoteResponse::from);
    }

    @Transactional(readOnly = true)
    public WrongAnswerNoteResponse getMyNote(Long noteId, Long userId) {
        return WrongAnswerNoteResponse.from(findMine(noteId, userId));
    }

    @Transactional(readOnly = true)
    public List<WrongAnswerNotePracticeResponse> getPracticeRecommendations(Long userId, Long subjectId, int size) {
        int normalizedSize = Math.max(1, Math.min(size, MAX_PRACTICE_SIZE));
        LocalDateTime now = LocalDateTime.now();
        return noteRepository.findPracticeRecommendations(
                        userId,
                        subjectId,
                        now,
                        PageRequest.of(0, normalizedSize)
                )
                .stream()
                .map(note -> WrongAnswerNotePracticeResponse.from(note, now))
                .toList();
    }

    public WrongAnswerNoteAnswerResponse viewAnswer(Long noteId, Long userId) {
        WrongAnswerNote note = findMine(noteId, userId);
        LocalDateTime now = LocalDateTime.now();
        note.recordReview(WrongAnswerReviewResult.ANSWER_VIEWED, now);
        reviewRepository.save(WrongAnswerNoteReview.create(
                note,
                userRepository.getReferenceById(userId),
                WrongAnswerReviewResult.ANSWER_VIEWED,
                null,
                now
        ));
        return WrongAnswerNoteAnswerResponse.from(note);
    }

    public WrongAnswerNoteReviewResponse recordReview(Long noteId, Long userId, WrongAnswerNoteReviewRequest request) {
        WrongAnswerReviewResult result = request.result();
        if (result == WrongAnswerReviewResult.ANSWER_VIEWED) {
            throw new IllegalArgumentException("답안보기 기록은 answer-view API를 사용하세요.");
        }

        WrongAnswerNote note = findMine(noteId, userId);
        User reviewer = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now();
        note.recordReview(result, now);
        WrongAnswerNoteReview review = WrongAnswerNoteReview.create(
                note,
                reviewer,
                result,
                blankToNull(request.memo()),
                now
        );
        return WrongAnswerNoteReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public Page<WrongAnswerNoteReviewResponse> getReviewLogs(Long noteId, Long userId, Pageable pageable) {
        findMine(noteId, userId);
        return reviewRepository.findByNoteIdAndNoteOwnerIdAndReviewResultNotOrderByReviewedAtDesc(
                noteId, userId, WrongAnswerReviewResult.ANSWER_VIEWED, pageable)
                .map(WrongAnswerNoteReviewResponse::from);
    }

    public WrongAnswerNoteResponse create(Long userId, WrongAnswerNoteCreateRequest request) {
        User owner = userRepository.getReferenceById(userId);
        SourceSnapshot source = resolveSource(userId, request);
        Subject subject = request.subjectId() != null
                ? findSubject(request.subjectId())
                : source.subject();

        String title = firstText(request.title(), source.title(), "오답노트");
        String questionContent = firstText(request.questionContent(), source.questionContent(), null);
        if (isBlank(questionContent)) {
            throw new IllegalArgumentException("문제 내용은 필수입니다.");
        }

        WrongAnswerNote note = WrongAnswerNote.create(
                owner,
                subject,
                title,
                questionContent,
                firstText(request.answerContent(), source.answerContent(), null),
                blankToNull(request.explanation()),
                blankToNull(request.wrongReason()),
                blankToNull(request.memo()),
                source.type(),
                source.sourceQuestionId(),
                source.sourceAnswerId(),
                source.sourceTitle(),
                normalizeTags(request.tags())
        );
        return WrongAnswerNoteResponse.from(noteRepository.save(note));
    }

    public WrongAnswerNoteResponse update(Long noteId, Long userId, WrongAnswerNoteUpdateRequest request) {
        WrongAnswerNote note = findMine(noteId, userId);
        Subject subject = request.subjectId() != null ? findSubject(request.subjectId()) : note.getSubject();

        String title = request.title() != null ? blankToNull(request.title()) : note.getTitle();
        String questionContent = request.questionContent() != null ? blankToNull(request.questionContent()) : note.getQuestionContent();
        if (isBlank(title)) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (isBlank(questionContent)) {
            throw new IllegalArgumentException("문제 내용은 필수입니다.");
        }

        note.update(
                subject,
                title,
                questionContent,
                request.answerContent() != null ? blankToNull(request.answerContent()) : note.getAnswerContent(),
                request.explanation() != null ? blankToNull(request.explanation()) : note.getExplanation(),
                request.wrongReason() != null ? blankToNull(request.wrongReason()) : note.getWrongReason(),
                request.memo() != null ? blankToNull(request.memo()) : note.getMemo(),
                request.tags() != null ? normalizeTags(request.tags()) : List.copyOf(note.getTags())
        );
        return WrongAnswerNoteResponse.from(note);
    }

    public void delete(Long noteId, Long userId) {
        WrongAnswerNote note = findMine(noteId, userId);
        note.delete(LocalDateTime.now());
    }

    private WrongAnswerNote findMine(Long noteId, Long userId) {
        return noteRepository.findByIdAndOwnerIdAndDeletedAtIsNull(noteId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "오답노트를 찾을 수 없습니다."));
    }

    private Subject findSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));
    }

    private SourceSnapshot resolveSource(Long userId, WrongAnswerNoteCreateRequest request) {
        WrongAnswerSourceType type = request.sourceType() != null ? request.sourceType() : WrongAnswerSourceType.DIRECT;
        return switch (type) {
            case DIRECT -> SourceSnapshot.direct();
            case QNA -> resolveQnaSource(request.sourceQuestionId(), request.sourceAnswerId());
            case AI -> resolveAiSource(userId, request.sourceQuestionId());
        };
    }

    private SourceSnapshot resolveQnaSource(Long questionId, Long answerId) {
        if (questionId == null) {
            throw new IllegalArgumentException("QnA 질문에서 오답노트를 만들려면 sourceQuestionId가 필요합니다.");
        }

        QnaQuestion question = qnaQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QnA 질문을 찾을 수 없습니다."));
        QnaAnswer answer = answerId != null
                ? qnaAnswerRepository.findById(answerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QnA 답변을 찾을 수 없습니다."))
                : qnaAnswerRepository.findFirstByQuestionIdAndAcceptedTrue(questionId).orElse(null);

        if (answer != null && !answer.getQuestion().getId().equals(question.getId())) {
            throw new IllegalArgumentException("해당 질문의 답변만 오답노트로 복사할 수 있습니다.");
        }

        return new SourceSnapshot(
                WrongAnswerSourceType.QNA,
                question.getId(),
                answer != null ? answer.getId() : null,
                question.getTitle(),
                question.getSubject(),
                question.getTitle(),
                question.getContent(),
                answer != null ? answer.getContent() : null
        );
    }

    private SourceSnapshot resolveAiSource(Long userId, Long questionId) {
        if (questionId == null) {
            throw new IllegalArgumentException("AI 질문에서 오답노트를 만들려면 sourceQuestionId가 필요합니다.");
        }

        AiQuestion question = aiQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 질문을 찾을 수 없습니다."));
        if (question.getUser() == null || !question.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 질문을 찾을 수 없습니다.");
        }

        return new SourceSnapshot(
                WrongAnswerSourceType.AI,
                question.getId(),
                null,
                "AI 질문",
                question.getSubject(),
                truncate(question.getQuestionText(), 80),
                question.getQuestionText(),
                question.getAnswerText()
        );
    }

    private static List<String> normalizeTags(Collection<String> tags) {
        if (tags == null) return List.of();

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = blankToNull(tag);
            if (value != null) {
                normalized.add(value);
            }
            if (normalized.size() >= MAX_TAG_COUNT) {
                break;
            }
        }
        return List.copyOf(normalized);
    }

    private static String normalizeKeyword(String keyword) {
        return blankToNull(keyword);
    }

    private static String firstText(String... values) {
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) return normalized;
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int max) {
        String text = Objects.requireNonNullElse(value, "");
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private record SourceSnapshot(
            WrongAnswerSourceType type,
            Long sourceQuestionId,
            Long sourceAnswerId,
            String sourceTitle,
            Subject subject,
            String title,
            String questionContent,
            String answerContent
    ) {
        static SourceSnapshot direct() {
            return new SourceSnapshot(WrongAnswerSourceType.DIRECT, null, null, null, null, null, null, null);
        }
    }
}
