package com.studyflow.domain.file.service;

import com.studyflow.domain.file.dto.response.FileUploadResponse;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.enums.FileCategory;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

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

    /** 허용 오디오 확장자 화이트리스트(강의실 듣기 자료). */
    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of(".mp3", ".wav", ".m4a", ".ogg", ".aac", ".flac");

    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;

    /**
     * 파일 저장소(개발=로컬 디스크, 운영=S3). {@code app.storage.type} 설정으로 구현이 주입된다.
     * FileService는 저장/읽기 위치를 모른 채 위임만 한다(멀티 인스턴스 무상태화).
     *
     * <p>경로 규약: {@code objectKey}(예: "chat/2026/06/22/uuid.png")는 저장소 내 키로 LOCAL·S3 공통이며
     * file_asset에 보관한다. {@code fileUrl}은 브라우저가 직접 GET 하는 공개 URL이다.</p>
     */
    private final StorageService storageService;

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
     * 프로필 이미지 업로드.
     *
     * <p>마이페이지 회원 정보에서 프로필 사진을 변경할 때 사용한다. 채팅 이미지와 동일한
     * 검증/저장 흐름이며 저장 폴더만 {@code profile}로 분리한다. 응답의 fileUrl을
     * 회원 정보 수정 API(PATCH /api/v1/users/me)의 profileImageUrl 필드에 전달한다.</p>
     *
     * <p>이전 프로필 이미지 파일은 별도로 삭제하지 않는다. 스토리지 용량이 충분한 MVP 단계에서는
     * 파일 보관 정책보다 구현 단순성을 우선하며, 추후 배치 또는 교체 시점 삭제로 개선 예정이다.</p>
     */
    @Transactional
    public FileUploadResponse uploadProfileImage(Long uploaderId, MultipartFile file) {
        return uploadImage(uploaderId, file, "profile");
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

            StorageService.Stored sf = storageService.store(bytes, folder, extension, file.getContentType());
            ImageSize imageSize = readImageSize(bytes);

            FileAsset fileAsset = FileAsset.createImage(
                    uploader,
                    originalFileName,
                    sf.storedFileName(),
                    storageService.provider(),
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
        // 활성 저장소(LOCAL/S3)에 위임해 objectKey로 읽는다. (경로 방어/네트워크 읽기는 구현이 담당)
        try {
            return storageService.read(asset.getObjectKey());
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
            StorageService.Stored sf = storageService.store(bytes, "chat", ".png", "image/png");
            ImageSize imageSize = readImageSize(bytes);

            FileAsset fileAsset = FileAsset.createImage(
                    owner,
                    sf.storedFileName(),
                    sf.storedFileName(),
                    storageService.provider(),
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
     * 선생님 인증 서류 업로드 (이미지 + PDF 허용).
     *
     * 반환된 fileUrl을 인증 신청 API(TeacherVerificationRequest.documentUrl)에 그대로 전달한다.
     */
    @Transactional
    public FileUploadResponse uploadVerificationDocument(Long uploaderId, MultipartFile file) {
        return uploadCourseAttachment(uploaderId, file, "verification");
    }

    /**
     * Classroom document upload. Used for whiteboard PDF backgrounds.
     */
    @Transactional
    public FileUploadResponse uploadClassroomDocument(Long uploaderId, MultipartFile file) {
        String extension = extractExtension(file == null ? null : file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_PDF_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("강의실에는 PDF 파일만 업로드할 수 있습니다.");
        }
        return uploadCourseAttachment(uploaderId, file, "classroom");
    }

    /**
     * 강의실 듣기 자료(오디오) 업로드 (이슈 #182). mp3/wav/m4a/ogg/aac/flac 허용, 최대 50MB.
     * 확장자 화이트리스트 + 매직바이트로 실제 오디오 여부를 검사하고 FileCategory.AUDIO로 저장한다.
     * 저장 경로: uploads/classroom-audio/yyyy/MM/dd/
     */
    @Transactional
    public FileUploadResponse uploadClassroomAudio(Long uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName).toLowerCase();
        if (!ALLOWED_AUDIO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("오디오 파일(mp3, wav, m4a, ogg, aac, flac)만 업로드할 수 있습니다.");
        }
        long maxSize = 50 * 1024 * 1024L; // 50MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("오디오 파일은 최대 50MB까지 업로드할 수 있습니다.");
        }
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        try {
            byte[] bytes = file.getBytes();
            if (!looksLikeAudio(bytes)) {
                throw new IllegalArgumentException("올바른 오디오 파일이 아닙니다.");
            }
            StorageService.Stored sf = storageService.store(bytes, "classroom-audio", extension, file.getContentType());
            FileAsset asset = FileAsset.createFile(uploader, originalFileName, sf.storedFileName(),
                    storageService.provider(), null, sf.objectKey(), sf.fileUrl(),
                    file.getContentType(), file.getSize(), FileCategory.AUDIO);
            FileAsset saved = fileAssetRepository.save(asset);
            return new FileUploadResponse(
                    saved.getId(), saved.getFileUrl(), saved.getThumbnailUrl(),
                    saved.getOriginalFileName(), saved.getContentType(),
                    saved.getFileSize(), null, null);
        } catch (IOException e) {
            throw new IllegalStateException("파일 업로드에 실패했습니다.", e);
        }
    }

    /** 흔한 오디오 컨테이너 시그니처(매직바이트)로 오디오 여부를 추정한다. */
    private static boolean looksLikeAudio(byte[] b) {
        if (b == null || b.length < 12) return false;
        int b0 = b[0] & 0xFF, b1 = b[1] & 0xFF, b2 = b[2] & 0xFF, b3 = b[3] & 0xFF;
        if (b0 == 0x49 && b1 == 0x44 && b2 == 0x33) return true;                 // "ID3" (mp3)
        if (b0 == 0xFF && (b1 & 0xE0) == 0xE0) return true;                      // MPEG/ADTS 프레임 동기 (mp3/aac)
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46                 // "RIFF"...."WAVE" (wav)
                && (b[8] & 0xFF) == 0x57 && (b[9] & 0xFF) == 0x41
                && (b[10] & 0xFF) == 0x56 && (b[11] & 0xFF) == 0x45) return true;
        if (b0 == 0x4F && b1 == 0x67 && b2 == 0x67 && b3 == 0x53) return true;   // "OggS" (ogg)
        if (b0 == 0x66 && b1 == 0x4C && b2 == 0x61 && b3 == 0x43) return true;   // "fLaC" (flac)
        if ((b[4] & 0xFF) == 0x66 && (b[5] & 0xFF) == 0x74                       // "ftyp" @4 (m4a/mp4)
                && (b[6] & 0xFF) == 0x79 && (b[7] & 0xFF) == 0x70) return true;
        return false;
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

            StorageService.Stored sf = storageService.store(bytes, folder, extension, contentType);

            FileCategory category = isPdf ? FileCategory.PDF : FileCategory.IMAGE;
            Integer width = null, height = null;
            if (isImage) {
                ImageSize size = readImageSize(bytes);
                width  = size.width();
                height = size.height();
            }

            FileAsset asset = isPdf
                    ? FileAsset.createFile(uploader, originalFileName, sf.storedFileName(),
                            storageService.provider(), null, sf.objectKey(), sf.fileUrl(),
                            contentType, fileSize, category)
                    : FileAsset.createImage(uploader, originalFileName, sf.storedFileName(),
                            storageService.provider(), null, sf.objectKey(), sf.fileUrl(), null,
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

    private record ImageSize(Integer width, Integer height) {}
}
