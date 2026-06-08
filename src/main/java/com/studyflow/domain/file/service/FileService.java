package com.studyflow.domain.file.service;

import com.studyflow.domain.file.dto.response.FileUploadResponse;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.enums.FileCategory;
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
     * 허용 이미지 확장자 화이트리스트. (소문자로 비교)
     *
     * Content-Type(클라이언트가 보낸 헤더, 위조 가능) 외에 확장자도 한 겹 더 검사한다.
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

    /** 허용 PDF 확장자 화이트리스트. 이미지와 동일한 패턴으로 관리한다. */
    private static final Set<String> ALLOWED_PDF_EXTENSIONS = Set.of(".pdf");

    /**
     * 로컬 저장 루트의 절대경로. 기동 시 한 번 고정해 저장/읽기가 항상 같은 기준점을 쓰게 한다.
     * (개발용 로컬 저장 — 운영에서는 S3로 교체 추천. file_asset 구조는 양쪽 모두 그대로 사용 가능)
     * (상대경로를 그때그때 해석하면 실행 위치(working directory)에 따라 경로가 달라질 수 있다)
     *
     * <p>경로 규약: {@code objectKey}(예: "chat/2026/06/04/uuid.png")는 S3 전환 시 버킷 내 키가
     * 되고, LOCAL에서는 이 루트("uploads/") 아래 상대경로다. 즉 로컬 루트 디렉터리가 버킷에
     * 해당하며, {@code fileUrl}("/uploads/" + objectKey)은 정적 서빙 경로일 뿐 저장 키가 아니다.</p>
     */
    private static final Path LOCAL_UPLOAD_ROOT = Paths.get("uploads").toAbsolutePath().normalize();

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
        return uploadImage(uploaderId, file, "chat");
    }

    /**
     * QnA(질문게시판) 이미지 업로드.
     *
     * <p>질문(학생)·답변(선생님)에 첨부할 이미지를 올린다. 채팅 이미지와 동일한 검증/저장 흐름이며
     * 저장 폴더만 {@code qna}로 분리한다. 응답의 fileId를 질문/답변 작성 요청의
     * {@code imageFileIds}에 담아 보낸다(여러 장이면 각 fileId를 모아 배열로).</p>
     */
    @Transactional
    public FileUploadResponse uploadQnaImage(Long uploaderId, MultipartFile file) {
        return uploadImage(uploaderId, file, "qna");
    }

    /**
     * 이미지 업로드 공통 로직. (저장 폴더만 {@code folder}로 분리)
     *
     * <p>허용 형식: JPEG/PNG/WEBP, 최대 10MB. Content-Type·확장자·매직바이트를 모두 검사한다.</p>
     */
    private FileUploadResponse uploadImage(Long uploaderId, MultipartFile file, String folder) {
        validateImage(file);

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName).toLowerCase();
        validateExtension(extension);   // 확장자 화이트리스트 검사

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

            StoredFile sf = storeLocalFile(bytes, folder, extension);
            ImageSize imageSize = readImageSize(bytes);

            FileAsset fileAsset = FileAsset.createImage(
                    uploader,
                    originalFileName,
                    sf.storedFileName(),
                    FileStorageProvider.LOCAL,
                    null,
                    sf.objectKey(),
                    sf.fileUrl(),
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

    /**
     * 저장된 이미지의 원본 바이트를 읽는다. (현재 LOCAL 저장소 전용)
     *
     * <p>AI vision 입력으로 넘기기 위해 사용한다. OpenAI가 로컬/비공개 파일 URL에 직접
     * 접근할 수 없으므로, 파일 내용을 읽어 base64로 전달하기 위함이다.</p>
     */
    public byte[] readImageBytes(FileAsset asset) {
        if (asset.getStorageProvider() != FileStorageProvider.LOCAL) {
            throw new IllegalStateException("로컬 저장 파일만 읽을 수 있습니다.");
        }
        // 저장 키(objectKey)를 절대 루트에 붙여 해석한다.
        // (fileUrl 문자열 파싱·작업 디렉터리 의존 제거 — 저장과 동일한 기준점 사용)
        Path path = LOCAL_UPLOAD_ROOT.resolve(asset.getObjectKey()).normalize();
        if (!path.startsWith(LOCAL_UPLOAD_ROOT)) {
            // "../" 등으로 루트를 벗어나는 키 방어 (DB가 오염됐더라도 임의 파일 읽기 차단)
            throw new IllegalStateException("잘못된 파일 경로입니다.");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("이미지 파일을 읽을 수 없습니다.", e);
        }
    }

    /**
     * AI가 생성한 이미지(PNG 바이트)를 저장하고 {@link FileAsset}을 반환한다.
     *
     * <p>업로드 이미지와 동일한 위치/규칙으로 저장하되, 사용자 업로드가 아니라 우리가 만든
     * PNG이므로 형식 검증은 생략한다. 반환된 FileAsset의 fileUrl을 AI 답변(마크다운 이미지)에 넣는다.</p>
     *
     * @param ownerId 이미지를 소유할(요청한) 사용자 id
     * @param bytes   생성된 PNG 바이트
     * @return 저장된 FileAsset
     */
    @Transactional
    public FileAsset saveGeneratedImage(Long ownerId, byte[] bytes) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        try {
            StoredFile sf = storeLocalFile(bytes, "chat", ".png");
            ImageSize imageSize = readImageSize(bytes);

            FileAsset fileAsset = FileAsset.createImage(
                    owner,
                    sf.storedFileName(),
                    sf.storedFileName(),
                    FileStorageProvider.LOCAL,
                    null,
                    sf.objectKey(),
                    sf.fileUrl(),
                    null,
                    "image/png",
                    (long) bytes.length,
                    imageSize.width(),
                    imageSize.height()
            );

            return fileAssetRepository.save(fileAsset);
        } catch (IOException e) {
            throw new IllegalStateException("생성 이미지 저장에 실패했습니다.", e);
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

    /**
     * 공지사항 첨부파일 업로드 (이미지 + PDF 허용).
     */
    @Transactional
    public FileUploadResponse uploadNoticeAttachment(Long uploaderId, MultipartFile file) {
        return uploadCourseAttachment(uploaderId, file, "notice");
    }

    /**
     * 게시판 첨부파일 업로드 (이미지 + PDF 허용).
     */
    @Transactional
    public FileUploadResponse uploadPostAttachment(Long uploaderId, MultipartFile file) {
        return uploadCourseAttachment(uploaderId, file, "post");
    }

    /**
     * 수업 관련 첨부파일 공통 업로드 로직 (이미지 + PDF 허용).
     *
     * - 이미지(JPEG/PNG/WEBP): 기존 채팅 이미지와 동일한 검증
     * - PDF: 매직바이트(25 50 44 46) 검사
     * - 저장 경로: uploads/{folder}/yyyy/MM/dd/
     */
    private FileUploadResponse uploadCourseAttachment(Long uploaderId, MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        // TODO: 공지/게시글 수정 시 이전 첨부파일(FileAsset + 실제 파일)이 삭제되지 않아 고아 파일이 누적됨.
        //       updateNotice/updatePost 호출 측에서 제거된 objectKey를 추적해 파일 삭제 로직을 추가해야 함.

        // 1단계: 확장자 화이트리스트 — Content-Type은 위조 가능하므로 확장자로 타입을 먼저 결정
        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName).toLowerCase();
        String contentType = file.getContentType();
        long fileSize = file.getSize();
        boolean isImage = ALLOWED_EXTENSIONS.contains(extension);   // .jpg/.jpeg/.png/.webp
        boolean isPdf   = ALLOWED_PDF_EXTENSIONS.contains(extension);

        if (!isImage && !isPdf) {
            throw new IllegalArgumentException("이미지(jpg/png/webp) 또는 PDF 파일만 업로드할 수 있습니다.");
        }

        long maxSize = 20 * 1024 * 1024L; // 20MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("파일은 최대 20MB까지 업로드할 수 있습니다.");
        }

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        try {
            byte[] bytes = file.getBytes();

            // 2단계: 매직바이트 — 실제 파일 내용이 확장자와 일치하는지 확인
            if (isImage) {
                validateMagicBytes(bytes);
            } else {
                // PDF 매직바이트 검사: %PDF → 25 50 44 46
                if (bytes.length < 4
                        || (bytes[0] & 0xFF) != 0x25 || (bytes[1] & 0xFF) != 0x50
                        || (bytes[2] & 0xFF) != 0x44 || (bytes[3] & 0xFF) != 0x46) {
                    throw new IllegalArgumentException("올바른 PDF 파일이 아닙니다.");
                }
            }

            StoredFile sf = storeLocalFile(bytes, folder, extension);

            FileCategory category = isPdf ? FileCategory.PDF : FileCategory.IMAGE;
            Integer width = null, height = null;
            if (isImage) {
                ImageSize size = readImageSize(bytes);
                width  = size.width();
                height = size.height();
            }

            FileAsset asset = isPdf
                    ? FileAsset.createFile(uploader, originalFileName, sf.storedFileName(),
                            FileStorageProvider.LOCAL, null, sf.objectKey(), sf.fileUrl(),
                            contentType, fileSize, category)
                    : FileAsset.createImage(uploader, originalFileName, sf.storedFileName(),
                            FileStorageProvider.LOCAL, null, sf.objectKey(), sf.fileUrl(), null,
                            contentType, fileSize, width, height);

            FileAsset saved = fileAssetRepository.save(asset);

            return new FileUploadResponse(
                    saved.getId(), saved.getFileUrl(), saved.getThumbnailUrl(),
                    saved.getOriginalFileName(), saved.getContentType(),
                    saved.getFileSize(), saved.getWidth(), saved.getHeight());
        } catch (IOException e) {
            throw new IllegalStateException("파일 업로드에 실패했습니다.", e);
        }
    }

    private StoredFile storeLocalFile(byte[] bytes, String folder, String extension) throws IOException {
        String datePath = LocalDate.now().format(DATE_PATH_FORMAT);
        Path uploadPath = LOCAL_UPLOAD_ROOT.resolve(folder).resolve(datePath);
        Files.createDirectories(uploadPath);
        String storedFileName = UUID.randomUUID() + extension;
        Files.copy(new ByteArrayInputStream(bytes), uploadPath.resolve(storedFileName),
                StandardCopyOption.REPLACE_EXISTING);
        return new StoredFile(
                storedFileName,
                folder + "/" + datePath + "/" + storedFileName,
                "/uploads/" + folder + "/" + datePath + "/" + storedFileName
        );
    }

    private record ImageSize(Integer width, Integer height) {}
    private record StoredFile(String storedFileName, String objectKey, String fileUrl) {}
}
