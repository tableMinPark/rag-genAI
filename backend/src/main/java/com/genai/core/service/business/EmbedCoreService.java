package com.genai.core.service.business;

import com.genai.core.type.CollectionType;

public interface EmbedCoreService {

    /**
     * 임베딩 문서 동기화
     *
     * @param collectionType 컬렉션 타입
     * @param fileId         파일 ID
     * @param categoryCode   카테고리 코드
     */
    void syncEmbedSources(CollectionType collectionType, long fileId, String categoryCode);

    /**
     * 임베딩 문서 삭제
     *
     * @param collectionType 컬렉션 타입
     * @param fileId         파일 ID
     */
    void deleteEmbedSources(CollectionType collectionType, long fileId, String categoryCode);
}