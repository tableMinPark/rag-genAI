package com.genai.core.service;

import com.genai.core.service.vo.QuestionVO;
import com.genai.core.type.CollectionType;

import java.util.List;

/**
 * 질문 & 답변 서비스
 */
public interface QuestionCoreService {

    /**
     * 질문 & 답변
     *
     * @param query     질의문
     * @param sessionId 사용자 ID
     * @param chatId    대화 ID
     * @return 답변 VO
     */
    QuestionVO question(String query, String sessionId, long chatId);

    /**
     * AI 질문 & 답변
     *
     * @param query         질의문
     * @param sessionId     사용자 ID
     * @param chatId        대화 ID
     * @param categoryCodes 카테고리 코드 목록
     * @return 답변 VO
     */
    QuestionVO questionAi(String query, String sessionId, long chatId, List<String> categoryCodes);

    /**
     * 나만의 AI 질문 & 답변
     *
     * @param query     질의문
     * @param sessionId 사용자 ID
     * @param chatId    대화 ID
     * @return 답변 VO
     */
    QuestionVO questionMyAi(String query, String sessionId, long chatId, long promptId);

    /**
     * 컬렉션 ID 기준 질문 & 답변
     *
     * @param query          질의문
     * @param sessionId      세션 ID
     * @param promptId       프롬프트 ID
     * @param collectionType 컬렉션
     * @param chatId         대화 ID
     * @param categoryCodes  검색 필터
     * @return 답변 VO
     */
    QuestionVO questionByCollectionId(String query, String sessionId, long chatId, long promptId, CollectionType collectionType, List<String> categoryCodes);
}
