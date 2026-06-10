package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeacherVerificationRequest {

    @NotNull(message = "서류 유형은 필수입니다.")
    private DocumentType documentType;

    @NotNull(message = "업로드된 파일 ID는 필수입니다.")
    private Long fileAssetId;

    @Size(max = 5000)
    private String description;

    @Size(max = 5000)
    private String awards;

    @Size(max = 5000)
    private String career;

    @Size(max = 300)
    private String education;
}
