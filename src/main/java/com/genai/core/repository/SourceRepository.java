package com.genai.core.repository;

import com.genai.core.repository.entity.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceRepository extends JpaRepository<SourceEntity, Long> {

    /**
     * 문서 삭제
     *
     * @param collectionId 컬렉션 ID
     * @param categoryCode 카테고리 코드
     * @param version      버전 정보
     */
    List<SourceEntity> findByCollectionIdAndCategoryCodeAndVersion(String collectionId, String categoryCode, Long version);

    /**
     * 문서 삭제
     *
     * @param collectionId 컬렉션 ID
     * @param categoryCode 카테고리 코드
     */
    List<SourceEntity> findByCollectionIdAndCategoryCode(String collectionId, String categoryCode);
}
