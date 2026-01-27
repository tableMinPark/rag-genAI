package com.genai.core.repository;

import com.genai.core.repository.entity.DocumentEntity;
import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.repository.wrapper.Search;
import com.genai.core.type.CollectionType;

import java.util.List;

public interface SearchRepository {

    /**
     * 키워드 검색 요청
     *
     * @param collectionType 컬렉션 타입
     * @param query          질의문
     * @param topK           top K
     * @param sessionId      세션 식별자
     * @param aliases          필터 코드 목록
     * @return 키워드 검색 결과 목록
     */
    <T extends DocumentEntity> List<Search<T>> keywordSearch(CollectionType collectionType, String query, int topK, String sessionId, List<String> aliases);

    /**
     * 벡터 검색 요청
     *
     * @param collectionType 컬렉션 타입
     * @param query          질의문
     * @param topK           top K
     * @param aliases          필터 코드 목록
     * @return 벡터 검색 결과 목록
     */
    <T extends DocumentEntity> List<Search<T>> vectorSearch(CollectionType collectionType, String query, int topK, List<String> aliases);

    /**
     * 검색 결과 리랭킹
     *
     * @param query     질의문
     * @param documents 검색 문서 목록
     * @return 리랭킹 문서 목록
     */
    List<Rerank> rerank(String query, List<Rerank> documents);

}