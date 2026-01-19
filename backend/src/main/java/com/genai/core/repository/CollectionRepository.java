package com.genai.core.repository;

import com.genai.core.repository.entity.CollectionEntity;
import com.genai.core.repository.entity.DocumentEntity;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository {

    /**
     * 컬렉션 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 컬렉션
     */
    Optional<CollectionEntity> findCollectionByCollectionId(String collectionId);

    /**
     * 벡터 변환
     *
     * @param collectionId     컬렉션 ID
     * @param documentEntities 변환 대상 문서 목록
     * @return 변환 완료 문서 목록
     */
    List<DocumentEntity> convertVector(String collectionId, List<DocumentEntity> documentEntities);

    /**
     * 데이터 색인
     *
     * @param collectionId     컬렉션 ID
     * @param documentEntities 색인 대상 문서 목록
     */
    void createIndex(String collectionId, List<DocumentEntity> documentEntities);

    /**
     * 색인 데이터 삭제
     *
     * @param collectionId 컬렉션 ID
     * @param chunkIds     chunkId 목록
     */
    void deleteIndex(String collectionId, List<String> chunkIds);

}
