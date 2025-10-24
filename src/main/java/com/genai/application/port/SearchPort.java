package com.genai.application.port;

import com.genai.application.enums.CollectionType;
import com.genai.application.domain.Document;
import com.genai.application.domain.Rerank;
import com.genai.application.domain.Search;

import java.util.List;

public interface SearchPort {

    /**
     * 검색 결과 리랭킹
     *
     * @param query     질의문
     * @param documents 검색 문서 목록
     * @return 리랭킹 문서 목록
     */
    List<Rerank> rerank(String query, List<Rerank> documents);

    /**
     * 컬렉션 키워드 검색 요청
     *
     * @param query     질의문
     * @param topK      top K
     * @param sessionId 세션 식별자
     * @return 키워드 검색 결과 목록
     */
    <T extends Document> List<Search<T>> keywordSearch(CollectionType collectionType, String collectionId, String query, int topK, String sessionId);

    /**
     * 컬렉션 벡터 검색 요청
     *
     * @param collectionType 컬렉션 타입
     * @param collectionId   컬렉션 ID
     * @param query          질의문
     * @param topK           top K
     * @return 벡터 검색 결과 목록
     */
    <T extends Document> List<Search<T>> vectorSearch(CollectionType collectionType, String collectionId, String query, int topK);
}