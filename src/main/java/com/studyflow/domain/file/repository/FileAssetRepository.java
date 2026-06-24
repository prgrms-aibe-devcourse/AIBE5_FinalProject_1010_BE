package com.studyflow.domain.file.repository;

import com.studyflow.domain.file.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    /**
     * 이미지 메시지를 보낼 때, 클라이언트가 전달한 fileId 목록을 한 번에 조회한다.
     */
    List<FileAsset> findByIdIn(List<Long> ids);

    /**
     * fileId 목록을 업로더와 함께 한 번에 조회한다.
     *
     * <p>업로더 소유권 검증({@code uploader.id == userId})을 트랜잭션/영속성 컨텍스트 밖에서도
     * 수행할 수 있도록 LAZY 연관인 {@code uploader}를 fetch join으로 미리 로딩한다.
     * 동시에 fileId별 개별 SELECT(N+1)를 단일 IN 쿼리로 대체한다.</p>
     */
    @Query("SELECT f FROM FileAsset f JOIN FETCH f.uploader WHERE f.id IN :ids")
    List<FileAsset> findByIdInWithUploader(@Param("ids") List<Long> ids);

    @Query("SELECT f FROM FileAsset f JOIN FETCH f.uploader WHERE f.id = :id")
    java.util.Optional<FileAsset> findByIdWithUploader(@Param("id") Long id);
}
