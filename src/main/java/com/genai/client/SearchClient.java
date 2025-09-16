package com.genai.client;

import com.genai.client.response.SearchResponse;
import com.genai.entity.LawEntity;

public interface SearchClient {

    /**
     * 키워드 검색 요청
     *
     * @param query     질의문
     * @param topK      top K
     * @param sessionId 세션 식별자
     * @return 키워드 검색 결과 목록
     */
    SearchResponse<LawEntity> lawKeywordSearch(String query, int topK, String sessionId);

    /**
     * 벡터 검색 요청
     *
     * @param query 질의문
     * @param topK  top K
     * @return 벡터 검색 결과 목록
     */
    SearchResponse<LawEntity> lawVectorSearch(String query, int topK);
}