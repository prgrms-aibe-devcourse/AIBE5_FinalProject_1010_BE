package com.studyflow.global.storage;

import com.studyflow.domain.file.enums.FileStorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * AWS S3 저장 구현(운영, 멀티 인스턴스). {@code app.storage.type=s3}일 때 활성화된다.
 *
 * <p>folder/yyyy/MM/dd/uuid.ext 키로 PutObject 하고, 브라우저가 직접 GET 할 공개 URL을 돌려준다.
 * 공개 URL은 {@code cloud.aws.s3.public-base-url}(CloudFront 등)이 있으면 그 베이스를, 없으면
 * S3 가상호스트 URL(https://{bucket}.s3.{region}.amazonaws.com)을 사용한다.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final S3Client s3Client;
    private final String bucket;
    private final String region;
    private final String publicBaseUrl;

    public S3StorageService(
            S3Client s3Client,
            @Value("${cloud.aws.s3.bucket}") String bucket,
            @Value("${cloud.aws.region.static}") String region,
            @Value("${cloud.aws.s3.public-base-url:}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public Stored store(byte[] bytes, String folder, String extension, String contentType) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMAT);
        String storedFileName = UUID.randomUUID() + extension;
        String objectKey = folder + "/" + datePath + "/" + storedFileName;

        PutObjectRequest.Builder req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey);
        String resolvedType = StringUtils.hasText(contentType) ? contentType : guessContentType(extension);
        if (resolvedType != null) {
            req.contentType(resolvedType);
        }
        s3Client.putObject(req.build(), RequestBody.fromBytes(bytes));

        return new Stored(storedFileName, objectKey, publicUrl(objectKey));
    }

    @Override
    public byte[] read(String objectKey) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build()).asByteArray();
    }

    @Override
    public FileStorageProvider provider() {
        return FileStorageProvider.S3;
    }

    private String publicUrl(String objectKey) {
        if (StringUtils.hasText(publicBaseUrl)) {
            return trimTrailingSlash(publicBaseUrl) + "/" + objectKey;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + objectKey;
    }

    private static String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /** 확장자 기반 Content-Type 추정(브라우저가 img/audio로 올바로 렌더하도록). */
    private static String guessContentType(String extension) {
        if (extension == null) return null;
        String ext = extension.toLowerCase(Locale.ROOT);
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".m4a" -> "audio/mp4";
            case ".ogg" -> "audio/ogg";
            case ".aac" -> "audio/aac";
            case ".flac" -> "audio/flac";
            default -> null;
        };
    }
}
