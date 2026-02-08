package com.genai.core.service.business;

import com.genai.core.service.business.vo.QuestionVO;
import com.genai.core.type.CollectionType;

import java.util.List;

public interface QuestionCoreService {

    /**
     * AI 질문 & 답변
     *
     * @param query         질의문
     * @param sessionId     사용자 ID
     * @param chatId        대화 ID
     * @param promptId      프롬프트 ID
     * @param categoryCodes 카테고리 코드 목록
     * @return 답변 VO
     */
    QuestionVO questionAi(String query, String sessionId, long chatId, long promptId, List<String> categoryCodes);

    /**
     * AI 질문 & 답변
     *
     * @param query         질의문
     * @param sessionId     사용자 ID
     * @param chatId        대화 ID
     * @param promptId      프롬프트 ID
     * @param categoryCode  카테고리 코드
     * @return 답변 VO
     */
    QuestionVO questionMyAi(String query, String sessionId, long chatId, long promptId, String categoryCode);

    /**
     * LLM 질문 & 답변
     *
     * @param query     질의문
     * @param sessionId 사용자 ID
     * @param chatId    대화 ID
     * @return 답변 VO
     */
    QuestionVO questionLlm(String query, String sessionId, long chatId, long promptId);

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

    /**
     * LLM Simulation 질문 & 답변
     *
     * @param query         질의문
     * @param sessionId     사용자 ID
     * @param chatId        대화 ID
     * @param context       참고 문서 (Context)
     * @param promptContent 프롬프트 본문
     * @param temperature   창의성
     * @param topP          일관성
     * @param maximumTokens 최대 토큰 수
     * @return 답변 VO
     */
    QuestionVO questionSimulation(String query, String sessionId, long chatId, String context, String promptContent, double temperature, double topP, int maximumTokens);

}
