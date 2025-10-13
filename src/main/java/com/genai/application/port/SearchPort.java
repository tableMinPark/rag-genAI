package com.genai.application.port;

import com.genai.application.domain.Document;
import com.genai.application.domain.Law;
import com.genai.application.domain.RerankDocument;

import java.util.List;

public interface SearchPort {

    /**
     * 법령 검색 결과 리랭킹
     *
     * @param query     질의문
     * @param documents 검색 결과 목록
     * @return 리랭킹 검색 결과 목록
     */
    List<RerankDocument<Law>> lawRerank(String query, List<Document<Law>> documents);

    /**
     * 키워드 검색 요청
     *
     * @param query     질의문
     * @param topK      top K
     * @param sessionId 세션 식별자
     * @return 키워드 검색 결과 목록
     */
    List<Document<Law>> lawKeywordSearch(String query, int topK, String sessionId);

    /**
     * 벡터 검색 요청
     *
     * @param query 질의문
     * @param topK  top K
     * @return 벡터 검색 결과 목록
     */
    List<Document<Law>> lawVectorSearch(String query, int topK);
}