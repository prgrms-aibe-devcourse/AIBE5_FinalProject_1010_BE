package com.studyflow.domain.ai.client;

import com.studyflow.domain.ai.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * OpenAI 이미지 생성(DALL·E) 호출 클라이언트.
 *
 * <p>학생이 "그림으로 그려줘 / 이미지로 보여줘"처럼 <b>그림 형태의 답변</b>을 요청하면,
 * 텍스트 LLM 대신 이미지 생성 모델을 호출해 그림을 만든다. 결과는 만료되는 임시 URL이 아니라
 * <b>base64 바이트</b>로 받아 우리 저장소에 보관할 수 있게 한다.(URL은 OpenAI에서 약 1시간 뒤
 * 사라지므로 대화 기록에 남기려면 직접 저장해야 한다.)</p>
 *
 * <p>모델·키 등은 {@code spring.ai.openai.*}로 자동 구성된 {@link ImageModel} 빈을 사용한다.</p>
 */
@Slf4j
@Component
public class OpenAiImageClient {

    /** 이미지 생성에 사용할 모델. (DALL·E 3은 한 번에 1장, 1024x1024 지원) */
    private static final String IMAGE_MODEL = "dall-e-3";
    private static final int IMAGE_SIZE = 1024;

    private final ImageModel imageModel;

    public OpenAiImageClient(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    /**
     * 프롬프트로 이미지를 생성해 PNG 바이트로 반환한다.
     *
     * @param prompt 그릴 내용(학생 질문 본문을 그대로 쓴다)
     * @return 생성된 PNG 이미지의 바이트
     * @throws AiServiceException 생성 실패 또는 빈 결과일 때
     */
    public byte[] generatePng(String prompt) {
        try {
            OpenAiImageOptions options = OpenAiImageOptions.builder()
                    .model(IMAGE_MODEL)
                    .N(1)
                    .width(IMAGE_SIZE)
                    .height(IMAGE_SIZE)
                    .responseFormat("b64_json") // URL 대신 base64로 받아 직접 저장
                    .build();

            ImageResponse response = imageModel.call(new ImagePrompt(prompt, options));
            Image output = (response.getResult() == null) ? null : response.getResult().getOutput();
            String b64 = (output == null) ? null : output.getB64Json();

            if (b64 == null || b64.isBlank()) {
                throw new AiServiceException("이미지 생성 결과가 비어 있습니다.");
            }
            return Base64.getDecoder().decode(b64);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            // 콘텐츠 정책 거부, 네트워크 오류, 타임아웃 등 모든 실패를 여기서 잡는다.
            log.error("OpenAI 이미지 생성 실패", e);
            throw new AiServiceException("이미지 생성에 실패했습니다.", e);
        }
    }
}
