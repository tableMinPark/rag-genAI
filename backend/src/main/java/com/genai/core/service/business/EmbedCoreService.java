package com.genai.core.service.business;

import com.genai.core.repository.entity.DocumentEntity;
import com.genai.core.service.business.vo.EmbedVO;
import com.genai.core.type.CollectionType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EmbedCoreService {

    EmbedVO syncEmbedSources(CollectionType collectionType, long fileId, String categoryCode);

    /**
     * 임베딩 문서 동기화
     *
     * @param collectionType 컬렉션 타입
     */
    Mono<Void> syncEmbedSources(CollectionType collectionType, List<DocumentEntity> documentEntities, List<String> deleteDocumentIds);

    /**
     * 임베딩 문서 삭제
     *
     * @param collectionType 컬렉션 타입
     * @param fileId         파일 ID
     */
    void deleteEmbedSources(CollectionType collectionType, long fileId, String categoryCode);
}