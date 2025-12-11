package com.genai.core.repository;

import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.PromptEntity;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ModelRepository {

    /**
     * 답변 생성 요청
     *
     * @param query        질의문
     * @param context      검색 결과 데이터
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 응답
     */
    List<AnswerEntity> generateAnswer(String query, String context, String sessionId, PromptEntity promptEntity);

    /**
     * 답변 실시간 생성 요청
     *
     * @param query        질의문
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 Flux
     */
    Flux<List<AnswerEntity>> generateStreamAnswer(String query, String sessionId, PromptEntity promptEntity);

    /**
     * 답변 실시간 생성 요청
     *
     * @param query        질의문
     * @param context      검색 결과 데이터
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 Flux
     */
    Flux<List<AnswerEntity>> generateStreamAnswer(String query, String context, String sessionId, PromptEntity promptEntity);

}