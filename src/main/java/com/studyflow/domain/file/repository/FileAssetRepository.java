package com.studyflow.domain.file.repository;

import com.studyflow.domain.file.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    /**
     * 이미지 메시지를 보낼 때, 클라이언트가 전달한 fileId 목록을 한 번에 조회한다.
     */
    List<FileAsset> findByIdIn(List<Long> ids);
}
