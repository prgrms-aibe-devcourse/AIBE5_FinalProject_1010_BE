package com.studyflow.global.storage;

import com.studyflow.domain.file.enums.FileStorageProvider;

import java.io.IOException;

/**
 * 업로드 파일 저장소 추상화 (이슈 #182 멀티 인스턴스 대응).
 *
 * <p>EC2 2대 + ALB 환경에서는 인스턴스 로컬 디스크에 저장하면 다른 인스턴스가 파일을 못 읽는다.
 * 그래서 저장/읽기를 이 인터페이스로 추상화하고, 설정 {@code app.storage.type}로 LOCAL(개발) /
 * S3(운영) 구현을 교체한다. {@code FileService}는 어떤 구현인지 모른 채 위임만 한다.</p>
 *
 * <p>{@code objectKey}는 저장소 내 키(예: "chat/2026/06/22/uuid.png")로 LOCAL·S3 공통이며
 * file_asset에 보관한다. {@code fileUrl}은 브라우저가 직접 GET 하는 공개 URL이다.</p>
 */
public interface StorageService {

    /** 바이트를 folder/yyyy/MM/dd/uuid.ext 키로 저장하고 메타를 반환한다. */
    Stored store(byte[] bytes, String folder, String extension, String contentType) throws IOException;

    /** objectKey로 저장된 바이트를 읽는다(AI vision 입력 등). */
    byte[] read(String objectKey) throws IOException;

    /** 이 저장소가 기록하는 provider 종류(file_asset.storage_provider에 저장). */
    FileStorageProvider provider();

    record Stored(String storedFileName, String objectKey, String fileUrl) {}
}
