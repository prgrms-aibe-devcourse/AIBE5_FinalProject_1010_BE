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
     * 개발용 로컬 저장 경로.
     *
     * 실제 운영에서는 S3로 교체하는 것을 추천한다.
     * file_asset 구조는 LOCAL이든 S3든 그대로 사용할 수 있다.
     */
    private static final String LOCAL_UPLOAD_DIR = "uploads/chat";

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
        String extension = extractExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + extension;

        try {
            /*
             * MultipartFile 의 InputStream 은 한 번 소비하면 재사용되지 않는다.
             * 예전에는 Files.copy 로 스트림을 소비한 뒤 readImageSize 에서 다시 getInputStream() 을 호출해서
             * 두 번째 스트림이 비어 ImageIO.read() 가 null 을 반환 → width/height 가 항상 null 로 저장됐다.
             * 그래서 파일 내용을 byte[] 로 한 번만 읽어, 저장과 이미지 크기 측정에 함께 사용한다.
             */
            byte[] bytes = file.getBytes();

            Path uploadPath = Paths.get(LOCAL_UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            Path storedPath = uploadPath.resolve(storedFileName);
            Files.copy(new ByteArrayInputStream(bytes), storedPath, StandardCopyOption.REPLACE_EXISTING);

            ImageSize imageSize = readImageSize(bytes);

            String objectKey = "chat/" + storedFileName;
            String fileUrl = "/uploads/chat/" + storedFileName;

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
