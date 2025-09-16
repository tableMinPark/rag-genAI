package com.genai.client;

import com.genai.client.response.AnswerResponse;
import com.genai.client.response.RerankResponse;
import com.genai.client.vo.DocumentVo;
import com.genai.entity.LawEntity;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ModelClient {

    /**
     * 법령 검색 결과 리랭킹
     *
     * @param query     질의문
     * @param documents 검색 결과 목록
     * @return 리랭킹 검색 결과 목록
     */
    RerankResponse<LawEntity> lawRerank(String query, List<DocumentVo<LawEntity>> documents);

    /**
     * 답변 생성 요청
     *
     * @param query     질의문
     * @param context   검색 결과 데이터
     * @param prompt    시스템 프롬 프트
     * @param sessionId 세션 식별자
     * @return 답변 응답
     */
    AnswerResponse generateAnswer(String query, String context, String prompt, String sessionId);

    /**
     * 답변 실시간 생성 요청
     *
     * @param query     질의문
     * @param context   검색 결과 데이터
     * @param prompt    시스템 프롬 프트
     * @param sessionId 세션 식별자
     * @return 답변 Flux
     */
    Flux<AnswerResponse> generateStreamAnswer(String query, String context, String prompt, String sessionId);
}