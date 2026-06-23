package com.studyflow.global.storage;

import com.studyflow.domain.file.enums.FileStorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 로컬 디스크 저장 구현(개발 기본값). {@code app.storage.type}가 없거나 "local"일 때 활성화된다.
 *
 * <p>저장 루트("uploads/") 아래 folder/yyyy/MM/dd/ 로 분산 저장하고, {@code /uploads/**} 정적 서빙
 * 경로를 fileUrl로 돌려준다(LocalFileWebConfig가 핸들링). 멀티 인스턴스에서는 S3 구현을 써야 한다.</p>
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    /** 저장 루트 절대경로(기동 시 고정 — 실행 위치에 무관하게 같은 기준점 사용). */
    private static final Path LOCAL_UPLOAD_ROOT = Paths.get("uploads").toAbsolutePath().normalize();
    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public Stored store(byte[] bytes, String folder, String extension, String contentType) throws IOException {
        String datePath = LocalDate.now().format(DATE_PATH_FORMAT);
        Path uploadPath = LOCAL_UPLOAD_ROOT.resolve(folder).resolve(datePath);
        Files.createDirectories(uploadPath);
        String storedFileName = UUID.randomUUID() + extension;
        Files.copy(new ByteArrayInputStream(bytes), uploadPath.resolve(storedFileName),
                StandardCopyOption.REPLACE_EXISTING);
        return new Stored(
                storedFileName,
                folder + "/" + datePath + "/" + storedFileName,
                "/uploads/" + folder + "/" + datePath + "/" + storedFileName
        );
    }

    @Override
    public byte[] read(String objectKey) throws IOException {
        // 저장 키를 절대 루트에 붙여 해석한다(작업 디렉터리 의존 제거).
        Path path = LOCAL_UPLOAD_ROOT.resolve(objectKey).normalize();
        if (!path.startsWith(LOCAL_UPLOAD_ROOT)) {
            // "../" 등으로 루트를 벗어나는 키 방어 (DB가 오염됐더라도 임의 파일 읽기 차단)
            throw new IllegalStateException("잘못된 파일 경로입니다.");
        }
        return Files.readAllBytes(path);
    }

    @Override
    public FileStorageProvider provider() {
        return FileStorageProvider.LOCAL;
    }
}
