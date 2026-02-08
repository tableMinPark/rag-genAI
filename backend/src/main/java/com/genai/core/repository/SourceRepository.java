package com.genai.core.repository;

import com.genai.core.repository.entity.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceRepository extends JpaRepository<SourceEntity, Long> {

    /**
     * 문서 조회
     *
     * @param collectionId 컬렉션 ID
     */
    List<SourceEntity> findByCollectionIdAndCategoryCode(String collectionId, String categoryCode);

    /**
     * 문서 조회
     *
     * @param fileDetailIds 파일 상세 ID 목록
     */
    List<SourceEntity> findByFileDetailIdIn(List<Long> fileDetailIds);

    /**
     * 문서 조회
     *
     * @param collectionId  컬렉션 ID
     * @param fileDetailIds 파일 상세 ID 목록
     */
    List<SourceEntity> findByCollectionIdAndCategoryCodeAndFileDetailIdIsNotIn(String collectionId, String categoryCode, List<Long> fileDetailIds);

    /**
     * 문서 조회
     *
     * @param collectionId  컬렉션 ID
     * @param fileDetailIds 파일 상세 ID 목록
     */
    List<SourceEntity> findByCollectionIdAndCategoryCodeAndFileDetailIdIn(String collectionId, String categoryCode, List<Long> fileDetailIds);
}