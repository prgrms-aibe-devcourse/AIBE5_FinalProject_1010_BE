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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * AI 질문 도메인 서비스.
 *
 * <p>외부 AI(OpenAI) 호출과 DB 저장/조회를 조율한다.</p>
 * <ul>
 *   <li>질문 요청: 과목 검증 → OpenAI 호출 → 질문+답변 저장 → 응답</li>
 *   <li>기록 조회: 내 질문 목록을 최신순으로 반환</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private final AiQuestionRepository aiQuestionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final FileAssetRepository fileAssetRepository;
    private final OpenAiClient openAiClient;

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
        //    (1단계: 텍스트만 전달. 2단계에서 images의 URL들을 vision으로 함께 넘기도록 확장)
        String answerText = openAiClient.ask(request.questionText());

        // 4) 질문 본문을 만들고, 첨부 이미지들을 순서대로 연결한다.
        AiQuestion question = AiQuestion.create(
                user,
                subject,
                request.questionText(),
                answerText
        );
        int order = 0;
        for (FileAsset image : images) {
            // create()가 내부에서 question.addAttachment를 호출 → cascade로 함께 저장됨
            AiQuestionAttachment.create(question, image, order++);
        }

        // 5) 저장 (attachments는 cascade = ALL로 함께 insert)
        AiQuestion saved = aiQuestionRepository.save(question);

        return AiQuestionResponse.from(saved);
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
        return fileIds.stream()
                .map(fileId -> {
                    FileAsset image = fileAssetRepository.findById(fileId)
                            .orElseThrow(() -> new IllegalArgumentException("첨부 이미지를 찾을 수 없습니다. (fileId: " + fileId + ")"));
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
     * 내 AI 질문 기록을 최신순으로 조회한다. (GET /api/v1/ai/questions)
     *
     * @param userId 인증된 사용자 id
     * @return 질문 기록 목록(최신순)
     */
    @Transactional(readOnly = true)
    public List<AiQuestionHistoryResponse> getMyHistory(Long userId) {
        return aiQuestionRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AiQuestionHistoryResponse::from)
                .toList();
    }
}
