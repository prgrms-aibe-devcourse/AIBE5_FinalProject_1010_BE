package com.studyflow.domain.file.service;

import com.studyflow.domain.file.dto.response.FileUploadResponse;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.enums.FileStorageProvider;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /**
     * 허용 확장자 화이트리스트. (소문자로 비교)
     *
     * Content-Type(클라이언트가 보낸 헤더, 위조 가능) 외에 확장자도 한 겹 더 검사한다.
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

    /**
     * 개발용 로컬 저장 경로.
     *
     * 실제 운영에서는 S3로 교체하는 것을 추천한다.
     * file_asset 구조는 LOCAL이든 S3든 그대로 사용할 수 있다.
     */
    private static final String LOCAL_UPLOAD_DIR = "uploads/chat";

    /**
     * 업로드 파일을 chat/년/월/일 하위 폴더로 분산 저장하기 위한 날짜 경로 포맷.
     *
     * 한 폴더에 파일이 무한정 쌓이는 것을 막고, 날짜별로 찾기/정리하기 쉽게 한다.
     * 예: chat/2026/05/29/{uuid}.png
     */
    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;

    /**
     * 채팅 이미지 업로드.
     *
     * 처리 흐름:
     * 1. 이미지 파일 검증
     * 2. 로컬 디렉터리에 파일 저장
     * 3. file_asset에 파일 메타데이터 저장
     * 4. 프론트가 메시지 전송 때 사용할 fileId 반환
     */
    @Transactional
    public FileUploadResponse uploadChatImage(Long uploaderId, MultipartFile file) {
        validateImage(file);

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName).toLowerCase();
        validateExtension(extension);   // 확장자 화이트리스트 검사
        String storedFileName = UUID.randomUUID() + extension;

        try {
            /*
             * MultipartFile 의 InputStream 은 한 번 소비하면 재사용되지 않는다.
             * 예전에는 Files.copy 로 스트림을 소비한 뒤 readImageSize 에서 다시 getInputStream() 을 호출해서
             * 두 번째 스트림이 비어 ImageIO.read() 가 null 을 반환 → width/height 가 항상 null 로 저장됐다.
             * 그래서 파일 내용을 byte[] 로 한 번만 읽어, 저장과 이미지 크기 측정에 함께 사용한다.
             */
            byte[] bytes = file.getBytes();

            // 매직바이트(파일 시그니처) 검사: 실제로 JPEG/PNG/WEBP 인지 확인한다.
            // Content-Type·확장자는 위조 가능하므로, 내용 자체를 검사해 위장 업로드를 막는다.
            validateMagicBytes(bytes);

            // chat/년/월/일 하위 폴더에 저장 (예: uploads/chat/2026/05/29/{uuid}.png)
            String datePath = LocalDate.now().format(DATE_PATH_FORMAT);   // "2026/05/29"

            Path uploadPath = Paths.get(LOCAL_UPLOAD_DIR, datePath);
            Files.createDirectories(uploadPath);

            Path storedPath = uploadPath.resolve(storedFileName);
            Files.copy(new ByteArrayInputStream(bytes), storedPath, StandardCopyOption.REPLACE_EXISTING);

            ImageSize imageSize = readImageSize(bytes);

            // objectKey / fileUrl 은 OS 와 무관하게 항상 '/' 구분자를 사용한다.
            String objectKey = "chat/" + datePath + "/" + storedFileName;
            String fileUrl = "/uploads/chat/" + datePath + "/" + storedFileName;

            FileAsset fileAsset = FileAsset.createImage(
                    uploader,
                    originalFileName,
                    storedFileName,
                    FileStorageProvider.LOCAL,
                    null,
                    objectKey,
                    fileUrl,
                    null,
                    file.getContentType(),
                    file.getSize(),
                    imageSize.width(),
                    imageSize.height()
            );

            FileAsset savedFile = fileAssetRepository.save(fileAsset);

            return new FileUploadResponse(
                    savedFile.getId(),
                    savedFile.getFileUrl(),
                    savedFile.getThumbnailUrl(),
                    savedFile.getOriginalFileName(),
                    savedFile.getContentType(),
                    savedFile.getFileSize(),
                    savedFile.getWidth(),
                    savedFile.getHeight()
            );
        } catch (IOException e) {
            throw new IllegalStateException("파일 업로드에 실패했습니다.", e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 없습니다.");
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }

        long maxSize = 10 * 1024 * 1024L;

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지는 최대 10MB까지 업로드할 수 있습니다.");
        }
    }

    /**
     * 확장자 화이트리스트 검사. (소문자 기준)
     *
     * 확장자가 없거나 허용 목록에 없으면 거부한다.
     */
    private void validateExtension(String extension) {
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다. (jpg, jpeg, png, webp만 가능)");
        }
    }

    /**
     * 매직바이트(파일 시그니처) 검사.
     *
     * 파일 앞부분 바이트를 보고 실제 포맷을 판별한다. Content-Type/확장자와 달리 위조하기 어렵다.
     * - PNG : 89 50 4E 47 0D 0A 1A 0A
     * - JPEG: FF D8 FF
     * - WEBP: "RIFF" .... "WEBP"
     *
     * 셋 중 어느 것도 아니면 실제 이미지가 아니라고 보고 거부한다.
     */
    private void validateMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new IllegalArgumentException("이미지 파일이 손상되었거나 너무 작습니다.");
        }

        boolean png = (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A;

        boolean jpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;

        boolean webp = (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49   // "RIFF"
                && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46
                && (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45        // "WEBP"
                && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50;

        if (!png && !jpeg && !webp) {
            throw new IllegalArgumentException("실제 이미지 파일이 아닙니다. (지원: JPEG, PNG, WEBP)");
        }
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }

        return originalFileName.substring(originalFileName.lastIndexOf("."));
    }

    private ImageSize readImageSize(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            if (image == null) {
                return new ImageSize(null, null);
            }

            return new ImageSize(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return new ImageSize(null, null);
        }
    }

    private record ImageSize(Integer width, Integer height) {
    }
}
