package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeacherVerificationRequest {

    @NotNull(message = "서류 유형은 필수입니다.")
    private DocumentType documentType;

    @NotBlank(message = "서류 URL은 필수입니다.")
    @Size(max = 500)
    @Pattern(
            regexp = "^.+\\.(jpg|jpeg|png|gif|bmp|webp|tiff|tif|heic|heif)$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "이미지 파일 URL만 허용됩니다. (jpg, jpeg, png, gif, bmp, webp, tiff, heic 등)"
    )
    private String documentUrl;

    @Size(max = 5000)
    private String description;
}
