package com.genai.core.service;

import com.genai.core.service.vo.SourceVO;
import com.genai.core.type.CollectionType;
import com.genai.core.exception.NotFoundException;

import java.util.List;

/**
 * 문서 임베딩 서비스
 */
public interface EmbedCoreService {

    /**
     * 나만의 AI 문서 임베딩
     *
     * @param projectId 프로젝트 ID
     * @param fileId    파일 ID
     */
    void embedMyAiSource(long projectId, long fileId) throws NotFoundException;

    /**
     * 나만의 AI 문서 삭제
     *
     * @param projectId 프로젝트 ID
     */
    void deleteEmbeddedMyAiSource(long projectId) throws NotFoundException;

    /**
     * 문서 임베딩
     *
     * @param collectionType 컬렉션
     * @param sourceVos      임베딩 문서 Vo 목록
     */
    void embedSources(CollectionType collectionType, List<SourceVO> sourceVos);
}