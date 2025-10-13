package com.genai.application.port;

import com.genai.application.domain.*;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ModelPort {

    /**
     * 답변 생성 요청
     *
     * @param query     질의문
     * @param context   검색 결과 데이터
     * @param sessionId 세션 식별자
     * @param prompt    프롬 프트
     * @return 답변 응답
     */
    List<Answer> generateAnswer(String query, String context, String sessionId, Prompt prompt);

    /**
     * 답변 실시간 생성 요청
     *
     * @param query     질의문
     * @param context   검색 결과 데이터
     * @param sessionId 세션 식별자
     * @param prompt    프롬 프트
     * @return 답변 Flux
     */
    Flux<List<Answer>> generateStreamAnswer(String query, String context, String sessionId, Prompt prompt);
}